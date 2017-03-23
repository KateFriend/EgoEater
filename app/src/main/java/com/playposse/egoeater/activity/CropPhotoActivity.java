package com.playposse.egoeater.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.edmodo.cropper.CropImageView;
import com.playposse.egoeater.ExtraConstants;
import com.playposse.egoeater.GlobalRouting;
import com.playposse.egoeater.R;
import com.playposse.egoeater.clientactions.ApiClientAction;
import com.playposse.egoeater.clientactions.UploadProfilePhotoClientAction;
import com.playposse.egoeater.storage.EgoEaterPreferences;

/**
 * An {@link android.app.Activity} that crops profile photos to fit the dimensions of the app.
 */
public class CropPhotoActivity extends ParentActivity {

    private static final String LOG_TAG = CropPhotoActivity.class.getSimpleName();

    /**
     * URL pattern to the FB profile photo of a user. The width is set to an arbitrary large size to
     * get the full size photo.
     */
    private static final String FB_PROFILE_PHOTO_URL =
            "https://graph.facebook.com/%1$s/picture?width=9999";

    private int photoIndex;
    private boolean hasFirstProfilePhoto;

    private CropImageView cropImageView;
    private Button cancelButton;
    private Button saveButton;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_crop_photo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoIndex = ExtraConstants.getPhotoIndex(getIntent());
        hasFirstProfilePhoto = EgoEaterPreferences.hasFirstProfilePhoto(this);

        cropImageView = (CropImageView) findViewById(R.id.cropImageView);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (!hasFirstProfilePhoto) {
            // If the user hasn't picked a profile photo yet, the user must pick a profile photo to
            // continue.
            cancelButton.setVisibility(View.GONE);
        }

        // TODO: Add to entry paths, one for FB photo and one for picking a photo from the gallery.

        loadFbProfilePhoto();

        setTitle(R.string.crop_photo_activity_title);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingProgress();
                new UploadProfilePhotoClientAction(
                        getApplicationContext(),
                        photoIndex,
                        cropImageView.getCroppedImage(),
                        new ApiClientAction.Callback<Void>() {
                            @Override
                            public void onResult(Void data) {
                                onCroppedPhotoUploadComplete();
                            }
                        })
                        .execute();
            }
        });
    }

    private void loadFbProfilePhoto() {
        String fbProfileId = EgoEaterPreferences.getUser(this).getFbProfileId();
        String fbPhotoUrl = String.format(FB_PROFILE_PHOTO_URL, fbProfileId);

        if (fbProfileId == null) {
            // Force the user to log in again and the app to download the FB profile id.
            GlobalRouting.onCloudError(this);
        }

        Glide
                .with(this)
                .load(fbPhotoUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        // Set the image manually, so that Glide won't do any resizing of the photo.
                        // For the cropping to work, the maximum resolution (not the screen
                        // resolution) has to be used.
                        cropImageView.setImageBitmap(bitmap);
                    }
                });

        Log.i(LOG_TAG, "loadFbProfilePhoto: " + fbPhotoUrl);
    }

    private void onCroppedPhotoUploadComplete() {
        dismissLoadingProgress();

        // TODO: Go to profile activity.
    }

}