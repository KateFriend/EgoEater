package com.playposse.egoeater.activity;

import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.playposse.egoeater.ExtraConstants;
import com.playposse.egoeater.R;
import com.playposse.egoeater.backend.egoEaterApi.model.UserBean;
import com.playposse.egoeater.clientactions.ApiClientAction;
import com.playposse.egoeater.clientactions.DeleteProfilePhotoClientAction;
import com.playposse.egoeater.clientactions.SwapProfilePhotosClientAction;
import com.playposse.egoeater.storage.EgoEaterPreferences;
import com.playposse.egoeater.storage.ProfileParcelable;
import com.playposse.egoeater.util.GlideUtil;
import com.playposse.egoeater.util.PhotoDragShadowBuilder;
import com.playposse.egoeater.util.ProfileFormatter;
import com.playposse.egoeater.util.SimpleAlertDialog;

import static android.view.View.*;

/**
 * A {@link Fragment} that lets the user edit his/her profile.
 */
public class ViewOwnProfileFragment extends Fragment {

    private static final String LOG_TAG = ViewOwnProfileFragment.class.getSimpleName();

    private ImageView profilePhoto0ImageView;
    private CardView photo1CardView;
    private ImageView profilePhoto1ImageView;
    private CardView photo2CardView;
    private ImageView profilePhoto2ImageView;
    private ImageView emptyPhoto1ImageView;
    private ImageView emptyPhoto2ImageView;
    private TextView headlineTextView;
    private TextView subHeadTextView;
    private TextView profileTextView;
    private FloatingActionButton editButton;

    public ViewOwnProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_view_own_profile, container, false);

        profilePhoto0ImageView = (ImageView) rootView.findViewById(R.id.profilePhoto0ImageView);
        photo1CardView = (CardView) rootView.findViewById(R.id.photo1CardView);
        profilePhoto1ImageView = (ImageView) rootView.findViewById(R.id.profilePhoto1ImageView);
        photo2CardView = (CardView) rootView.findViewById(R.id.photo2CardView);
        profilePhoto2ImageView = (ImageView) rootView.findViewById(R.id.profilePhoto2ImageView);
        emptyPhoto1ImageView = (ImageView) rootView.findViewById(R.id.emptyPhoto1ImageView);
        emptyPhoto2ImageView = (ImageView) rootView.findViewById(R.id.emptyPhoto2ImageView);
        headlineTextView = (TextView) rootView.findViewById(R.id.headlineTextView);
        subHeadTextView = (TextView) rootView.findViewById(R.id.subHeadTextView);
        profileTextView = (TextView) rootView.findViewById(R.id.profileTextView);
        editButton = (FloatingActionButton) rootView.findViewById(R.id.editButton);

        refreshPhotos();

        UserBean userBean = EgoEaterPreferences.getUser(getContext());
        ProfileParcelable profile = new ProfileParcelable(userBean);
        headlineTextView.setText(ProfileFormatter.formatNameAndAge(getContext(), profile));
        subHeadTextView.setText(ProfileFormatter.formatCityStateAndDistance(getContext(), profile));
        profileTextView.setText(profile.getProfileText());

        editButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                startActivity(new Intent(getContext(), EditProfileActivity.class));
            }
        });

        return rootView;
    }

    private void refreshPhotos() {
        initProfilePhoto(
                0,
                profilePhoto0ImageView,
                null,
                null,
                EgoEaterPreferences.getProfilePhotoUrl0(getContext()));
        initProfilePhoto(
                1,
                profilePhoto1ImageView,
                emptyPhoto1ImageView,
                photo1CardView,
                EgoEaterPreferences.getProfilePhotoUrl1(getContext()));
        initProfilePhoto(
                2,
                profilePhoto2ImageView,
                emptyPhoto2ImageView,
                photo2CardView,
                EgoEaterPreferences.getProfilePhotoUrl2(getContext()));
    }

    private void initProfilePhoto(
            final int photoIndex,
            ImageView imageView,
            @Nullable ImageView emptyView,
            @Nullable CardView cardView,
            String photoUrl) {

        if (photoUrl != null) {
            GlideUtil.load(imageView, photoUrl);
            imageView.setVisibility(VISIBLE);
            if (emptyView != null) {
                emptyView.setVisibility(GONE);
            }
            if (cardView != null) {
                cardView.setVisibility(VISIBLE);
            }
        } else {
            imageView.setVisibility(GONE);
            if (emptyView != null) {
                emptyView.setVisibility(VISIBLE);
            }
            if (cardView != null) {
                cardView.setVisibility(GONE);
            }
        }

        OnClickListener clickListenerToDialog = new OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleAlertDialog.confirmPhoto(
                        getContext(),
                        new Runnable() {
                            @Override
                            public void run() {
                                pickPhoto(photoIndex);
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                                deletePhoto(photoIndex);
                            }
                        });
            }
        };
        OnClickListener clickListenerToPickActivity = new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto(photoIndex);
            }
        };
        if (photoIndex == 0) {
            imageView.setOnClickListener(clickListenerToPickActivity);
        } else {
            imageView.setOnClickListener(clickListenerToDialog);
        }
        if (emptyView != null) {
            emptyView.setOnClickListener(clickListenerToPickActivity);
        }

        enableDragAndDrop(imageView, emptyView, photoIndex);
    }

    private void pickPhoto(int photoIndex) {
        Intent intent = ExtraConstants.createCropPhotoIntent(getContext(), photoIndex);
        startActivity(intent);
    }

    private void deletePhoto(final int photoIndex) {
        ((ActivityWithProgressDialog) getActivity()).showLoadingProgress();
        new DeleteProfilePhotoClientAction(
                getContext(),
                photoIndex,
                new ApiClientAction.Callback<Void>() {
                    @Override
                    public void onResult(Void data) {
                        ((ActivityWithProgressDialog) getActivity()).dismissLoadingProgress();
                        clearPhotoSlot(photoIndex);
                    }
                }).execute();
    }

    private void clearPhotoSlot(int photoIndex) {
        if ((photoIndex == 1) && (EgoEaterPreferences.getProfilePhotoUrl1(getContext()) != null)) {
            // We actually need to clear the next photo slot and move the photo from slot 2 to slot
            // 1.
            initProfilePhoto(
                    1,
                    profilePhoto1ImageView,
                    emptyPhoto1ImageView,
                    photo1CardView,
                    EgoEaterPreferences.getProfilePhotoUrl1(getContext()));

            photoIndex = 2;
        }


        switch (photoIndex) {
            case 1:
                profilePhoto1ImageView.setImageBitmap(null);
                photo1CardView.setVisibility(GONE);
                profilePhoto1ImageView.setVisibility(GONE);
                emptyPhoto1ImageView.setVisibility(VISIBLE);
                break;
            case 2:
                profilePhoto2ImageView.setImageBitmap(null);
                photo2CardView.setVisibility(GONE);
                profilePhoto2ImageView.setVisibility(GONE);
                emptyPhoto2ImageView.setVisibility(VISIBLE);
                break;
        }
    }

    private void enableDragAndDrop(
            final ImageView imageView,
            @Nullable ImageView emptyPhotoImageView,
            final int photoIndex) {

        // Set long click listener to start drags on the particular ImageView.
        imageView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipData clipData = ClipData.newPlainText(null, Integer.toString(photoIndex));

                view.startDrag(
                        clipData,
                        new PhotoDragShadowBuilder(imageView),
                        null,
                        0);
                return true;
            }
        });

        // Set listener to receive drag drops on the particular view.
        PhotoDragListener dragListener = new PhotoDragListener(imageView, photoIndex);
        imageView.setOnDragListener(dragListener);
        if (emptyPhotoImageView != null) {
            emptyPhotoImageView.setOnDragListener(dragListener);
        }
    }

    /**
     * Swaps photos that are in two different slots.
     */
    private void swapPhotos(int sourcePhotoIndex, int destPhotoIndex) {
        // Ignore if the same source and destination.
        if (sourcePhotoIndex == destPhotoIndex) {
            return;
        }

        ((ActivityWithProgressDialog) getActivity()).showLoadingProgress();

        new SwapProfilePhotosClientAction(
                getContext(),
                sourcePhotoIndex,
                destPhotoIndex,
                new ApiClientAction.Callback<UserBean>() {
                    @Override
                    public void onResult(UserBean data) {
                        onSwapPhotosComplete();
                    }
                }).execute();
    }

    private void onSwapPhotosComplete() {
        // Rebuild the data in the UI.
        refreshPhotos();

        ((ActivityWithProgressDialog) getActivity()).dismissLoadingProgress();
    }

    private int getAvailableDragTargetTint() {
        return ContextCompat.getColor(getContext(), R.color.availableDragTargetTint);
    }

    private int getActiveDragTargetTint() {
        return ContextCompat.getColor(getContext(), R.color.activeDragTargetTint);
    }

    /**
     * A {@link OnDragListener} that listens for photos to be dropped into different slots.
     */
    private class PhotoDragListener implements OnDragListener {
        private final ImageView imageView;
        private final int photoIndex;

        private PhotoDragListener(ImageView imageView, int photoIndex) {
            this.imageView = imageView;
            this.photoIndex = photoIndex;
        }

        @Override
        public boolean onDrag(View view, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    imageView.setColorFilter(getAvailableDragTargetTint());
                    imageView.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    imageView.setColorFilter(getActiveDragTargetTint());
                    imageView.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    // Ignore the event
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    imageView.setColorFilter(getAvailableDragTargetTint());
                    imageView.invalidate();
                    return true;
                case DragEvent.ACTION_DROP:
                    String photoIndexStr =
                            event.getClipData().getItemAt(0).getText().toString();
                    int sourcePhotoIndex = Integer.parseInt(photoIndexStr);
                    swapPhotos(sourcePhotoIndex, photoIndex);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    imageView.clearColorFilter();
                    imageView.invalidate();
                    return true;
                default:
                    Log.e(LOG_TAG, "onDrag: Unknown drag action type: " + event.getAction());
                    return false;
            }
        }
    }
}
