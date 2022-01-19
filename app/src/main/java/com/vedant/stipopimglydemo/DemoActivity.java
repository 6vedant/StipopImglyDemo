package com.vedant.stipopimglydemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.stipop.Stipop;
import io.stipop.StipopDelegate;
import io.stipop.extend.StipopImageView;
import io.stipop.model.SPPackage;
import io.stipop.model.SPSticker;
import ly.img.android.pesdk.VideoEditorSettingsList;
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic;
import ly.img.android.pesdk.assets.font.basic.FontPackBasic;
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic;
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic;
import ly.img.android.pesdk.assets.sticker.emoticons.StickerPackEmoticons;
import ly.img.android.pesdk.assets.sticker.shapes.StickerPackShapes;
import ly.img.android.pesdk.backend.decoder.ImageSource;
import ly.img.android.pesdk.backend.model.EditorSDKResult;
import ly.img.android.pesdk.backend.model.state.LoadSettings;
import ly.img.android.pesdk.backend.model.state.VideoEditorSaveSettings;
import ly.img.android.pesdk.backend.model.state.manager.SettingsList;
import ly.img.android.pesdk.ui.activity.VideoEditorBuilder;
import ly.img.android.pesdk.ui.model.state.UiConfigFilter;
import ly.img.android.pesdk.ui.model.state.UiConfigFrame;
import ly.img.android.pesdk.ui.model.state.UiConfigOverlay;
import ly.img.android.pesdk.ui.model.state.UiConfigSticker;
import ly.img.android.pesdk.ui.model.state.UiConfigText;
import ly.img.android.pesdk.ui.panels.item.ImageStickerItem;
import ly.img.android.pesdk.ui.panels.item.StickerCategoryItem;
import ly.img.android.serializer._3.IMGLYFileWriter;

public class DemoActivity extends AppCompatActivity implements StipopDelegate {

    public static int VESDK_RESULT = 1;
    public static int GALLERY_RESULT = 2;
    StipopImageView stipopImageView;

    private static ArrayList<SPSticker> stipopStickers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        stipopStickers.clear();

        stipopImageView = (StipopImageView) findViewById(R.id.stipopImg);
        Stipop.Companion.connect(this, stipopImageView, "1234", "en", "US", this);

        findViewById(R.id.textview_open_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSystemGalleryToSelectAVideo();
            }
        });

        findViewById(R.id.stipopImg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Stipop.Companion.showKeyboard();
            }
        });

    }

    private void openSystemGalleryToSelectAVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
        try {
            startActivityForResult(intent, GALLERY_RESULT);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "No Gallery APP installed",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openEditor(Uri inputSource) {

        VideoEditorSettingsList settingsList;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settingsList = createVesdkSettingsList();
        } else {
            Toast.makeText(this, "Video support needs Android 4.3", Toast.LENGTH_LONG).show();
            return;
        }

        // Set input video
        settingsList.getSettingsModel(LoadSettings.class).setSource(inputSource);

        // Set output video
        settingsList.getSettingsModel(VideoEditorSaveSettings.class).setOutputToGallery(Environment.DIRECTORY_DCIM);

        new VideoEditorBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, VESDK_RESULT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            // Open Editor with some uri in this case with a video selected from the system gallery.
            Uri selectedVideo = intent.getData();
            openEditor(selectedVideo);

        } else if (resultCode == RESULT_OK && requestCode == VESDK_RESULT) {
            // Editor has saved a video.
            EditorSDKResult data = new EditorSDKResult(intent);

            Uri resultURI = data.getResultUri();
            Uri sourceURI = data.getSourceUri();

            Log.i("PESDK", "Source video is located here " + sourceURI);
            Log.i("PESDK", "Result video is located here " + resultURI);

            // TODO: Do something with the result video

            // OPTIONAL: read the latest state to save it as a serialisation
            SettingsList lastState = data.getSettingsList();
            try {
                new IMGLYFileWriter(lastState).writeJson(new File(
                        getExternalFilesDir(null),
                        "serialisationReadyToReadWithPESDKFileReader.json"
                ));
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (resultCode == RESULT_CANCELED && requestCode == VESDK_RESULT) {
            // Editor was canceled
            EditorSDKResult data = new EditorSDKResult(intent);

            Uri sourceURI = data.getSourceUri();
            // TODO: Do something with the source...
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private VideoEditorSettingsList createVesdkSettingsList() {
        // Create an empty new SettingsList and apply the changes on this reference.
        VideoEditorSettingsList settingsList = new VideoEditorSettingsList();

        // If you include our asset Packs and you use our UI you also need to add them to the UI Config,
        // otherwise they are only available for the backend
        // See the specific feature sections of our guides if you want to know how to add your own Assets.

        settingsList.getSettingsModel(UiConfigFilter.class).setFilterList(
                FilterPackBasic.getFilterPack()
        );

        settingsList.getSettingsModel(UiConfigText.class).setFontList(
                FontPackBasic.getFontPack()
        );

        settingsList.getSettingsModel(UiConfigFrame.class).setFrameList(
                FramePackBasic.getFramePack()
        );

        settingsList.getSettingsModel(UiConfigOverlay.class).setOverlayList(
                OverlayPackBasic.getOverlayPack()
        );
        addStipopStickersToImgly(settingsList);


        return settingsList;
    }

    public void addStipopStickersToImgly(SettingsList settingsList) {
        settingsList.getSettingsModel(UiConfigSticker.class).setStickerLists(
                StickerPackEmoticons.getStickerCategory(),
                StickerPackShapes.getStickerCategory()
        );

        List<ImageStickerItem> imageStickerItems = new ArrayList<>();
        imageStickerItems.clear();
        UiConfigSticker uiConfigSticker = settingsList.getSettingsModel(UiConfigSticker.class);
        for (SPSticker spSticker : stipopStickers) {
            ImageStickerItem imageStickerItem = new ImageStickerItem(""+spSticker.getStickerId(), spSticker.getKeyword(), ImageSource.create(Uri.parse(spSticker.getStickerImg())));
            //uiConfigSticker.addToPersonalStickerList(new ImageStickerItem("id"+spSticker.getStickerId(), spSticker.getKeyword(), ImageSource.create(Uri.parse(spSticker.getStickerImg()))));
            imageStickerItems.add(imageStickerItem);
        }
        StickerCategoryItem stickerCategoryItem = new StickerCategoryItem("stipop", R.string.app_name, ImageSource.create(R.mipmap.ic_sticker_normal), imageStickerItems);
        uiConfigSticker.setStickerLists(stickerCategoryItem);


    }


    @Override
    public boolean canDownload(@NonNull SPPackage spPackage) {


        return true;
    }

    @Override
    public boolean onStickerSelected(@NonNull SPSticker spSticker) {
        stipopStickers.add(spSticker);
        return true;
    }
}