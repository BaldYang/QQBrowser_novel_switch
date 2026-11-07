package com.QQBrowsernovelswitch.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.core.Announces;
import com.QQBrowsernovelswitch.core.ConversionViewModel;
import com.QQBrowsernovelswitch.core.LinkUtils;
import com.QQBrowsernovelswitch.core.NovelConverter;
import com.QQBrowsernovelswitch.core.NovelRepository;
import com.QQBrowsernovelswitch.core.NovelViewModel;
import com.QQBrowsernovelswitch.core.ReadingEnvironmentChecker;
import com.QQBrowsernovelswitch.core.ShizukuManager;
import com.QQBrowsernovelswitch.core.Update;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class SwitchActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 102;

    private boolean initialCheckDone;
    private boolean pendingResultToast;
    private boolean waitingForService;
    private ShizukuManager.OnServiceReadyListener serviceReadyListener;
    private boolean hasLoadedNovels;
    private Toast loadingToast;
    private NovelRepository.Novel pendingConversionNovel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable environmentRetry = this::tryLoadNovels;
    private final Runnable serviceWaitTimeout = () -> {
        waitingForService = false;
        removeServiceReadyListener();
    };

    private NovelViewModel novelViewModel;
    private ConversionViewModel conversionViewModel;
    private View emptyNovelView;
    private View novelListHeader;
    private View initialHintView;
    private RecyclerView novelList;
    private NovelAdapter novelAdapter;

    private AlertDialog progressDialog;
    private ProgressBar conversionProgress;
    private TextView conversionPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_switch);

        emptyNovelView = findViewById(R.id.emptyNovelView);
        novelListHeader = findViewById(R.id.novelListHeader);
        initialHintView = findViewById(R.id.initialHintView);
        novelList = findViewById(R.id.novelList);
        novelAdapter = new NovelAdapter(this::startConversion);
        novelList.setLayoutManager(new LinearLayoutManager(this));
        novelList.setAdapter(novelAdapter);

        novelViewModel = new ViewModelProvider(this).get(NovelViewModel.class);
        novelViewModel.getNovels().observe(this, this::onNovelsLoaded);

        conversionViewModel = new ViewModelProvider(this).get(ConversionViewModel.class);
        conversionViewModel.getProgress().observe(this, this::onConversionProgress);
        conversionViewModel.getResult().observe(this, result -> {
            if (result != null && conversionViewModel.takeResult()) {
                onConversionSuccess(result);
            }
        });
        conversionViewModel.getFailure().observe(this, message -> {
            if (message != null && conversionViewModel.takeFailure()) {
                onConversionFailure(message);
            }
        });
        conversionViewModel.getCancelled().observe(this, cancelled -> {
            if (Boolean.TRUE.equals(cancelled)) {
                dismissProgress();
                Toast.makeText(this, R.string.convert_cancelled, Toast.LENGTH_SHORT).show();
            }
        });

        Update.checkUpdate(this, "SwitchActivity");
        Announces.checkAnnounces(this, "SwitchActivity");

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SwitchActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        settingsButton.setOnLongClickListener(v -> {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.tencent.mtt");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    return true;
                } else {
                    Toast.makeText(this, R.string.no_qq_browser, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });

        Button checkNovelButton = findViewById(R.id.checkNovelButton);
        checkNovelButton.setOnClickListener(v -> {
            ReadingEnvironmentChecker.check(this);
            refreshNovels();
        });

        Button instructionButton = findViewById(R.id.instructionsButton);
        instructionButton.setOnClickListener(v ->
                new Instructions(SwitchActivity.this).showInstructionsDialog());

        Button authorButton = findViewById(R.id.authorButton);
        authorButton.setOnClickListener(v ->
                startActivity(new Intent(SwitchActivity.this, AuthorActivity.class)));

        if (conversionViewModel.isRunning()) {
            showProgressDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!initialCheckDone) {
            initialCheckDone = true;
            ReadingEnvironmentChecker.check(this);
        } else if (ReadingEnvironmentChecker.consumePendingShizukuReturn()) {
            ReadingEnvironmentChecker.check(this);
        } else {
            ReadingEnvironmentChecker.checkOnResume(this);
        }
        tryLoadNovels();
        mainHandler.removeCallbacks(environmentRetry);
        mainHandler.postDelayed(environmentRetry, 1500);
        if (conversionViewModel.isRunning() && progressDialog == null) {
            showProgressDialog();
        }
    }

    private void tryLoadNovels() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean ready = ReadingEnvironmentChecker.canReadNovels(this);
        if (initialHintView != null) {
            initialHintView.setVisibility(
                    !ready && !hasLoadedNovels ? View.VISIBLE : View.GONE);
        }
        if (ready && novelViewModel.ensureLoaded(this)) {
            pendingResultToast = true;
            showLoadingToast();
        }
    }

    private void refreshNovels() {
        if (!ReadingEnvironmentChecker.canReadNovels(this)) {
            return;
        }
        if (initialHintView != null) {
            initialHintView.setVisibility(View.GONE);
        }
        if (novelViewModel.refresh(this)) {
            pendingResultToast = true;
            showLoadingToast();
        }
    }

    private void showLoadingToast() {
        if (loadingToast != null) {
            loadingToast.cancel();
        }
        loadingToast = Toast.makeText(this, R.string.searching, Toast.LENGTH_SHORT);
        loadingToast.show();
    }

    private void onNovelsLoaded(@NonNull List<NovelRepository.Novel> novels) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (loadingToast != null) {
            loadingToast.cancel();
            loadingToast = null;
        }
        if (!novels.isEmpty()) {
            hasLoadedNovels = true;
        }
        renderNovels(novels);
        if (!pendingResultToast) {
            return;
        }
        pendingResultToast = false;
        Toast.makeText(this,
                novels.isEmpty() ? getString(R.string.empty_novels)
                        : getString(R.string.search_result_count, novels.size()),
                Toast.LENGTH_SHORT).show();
    }

    private void renderNovels(@NonNull List<NovelRepository.Novel> novels) {
        if (initialHintView != null) {
            initialHintView.setVisibility(View.GONE);
        }
        if (novels.isEmpty()) {
            if (!ReadingEnvironmentChecker.canReadNovels(this) && !hasLoadedNovels) {
                if (initialHintView != null) {
                    initialHintView.setVisibility(View.VISIBLE);
                }
                emptyNovelView.setVisibility(View.GONE);
                novelListHeader.setVisibility(View.GONE);
                novelList.setVisibility(View.GONE);
                return;
            }
            emptyNovelView.setVisibility(View.VISIBLE);
            novelListHeader.setVisibility(View.GONE);
            novelList.setVisibility(View.GONE);
            novelAdapter.submit(novels);
            return;
        }
        emptyNovelView.setVisibility(View.GONE);
        novelListHeader.setVisibility(View.VISIBLE);
        novelList.setVisibility(View.VISIBLE);
        novelAdapter.submit(novels);
    }

    private void startConversion(@NonNull NovelRepository.Novel novel) {
        if (conversionViewModel.isRunning() || pendingConversionNovel != null
                || waitingForService) {
            if (progressDialog == null && conversionViewModel.isRunning()) {
                showProgressDialog();
            }
            return;
        }
        if (!ReadingEnvironmentChecker.canConvertNovels(this)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                ReadingEnvironmentChecker.requestStoragePermissions(this);
            } else {
                ReadingEnvironmentChecker.check(this);
            }
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingConversionNovel = novel;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIFICATION_PERMISSION);
            return;
        }
        beginConversion(novel);
    }

    private void beginConversion(@NonNull NovelRepository.Novel novel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !ShizukuManager.isReady()) {
            waitingForService = true;
            ShizukuManager.bindUserService();
            mainHandler.removeCallbacks(serviceWaitTimeout);
            mainHandler.postDelayed(serviceWaitTimeout, 9000);
            ShizukuManager.OnServiceReadyListener listener = () -> runOnUiThread(() -> {
                mainHandler.removeCallbacks(serviceWaitTimeout);
                waitingForService = false;
                serviceReadyListener = null;
                if (isFinishing() || isDestroyed() || conversionViewModel.isRunning()) {
                    return;
                }
                startConversionNow(novel);
            });
            serviceReadyListener = listener;
            ShizukuManager.addOnServiceReadyListener(listener);
            return;
        }
        startConversionNow(novel);
    }

    private void removeServiceReadyListener() {
        if (serviceReadyListener != null) {
            ShizukuManager.removeOnServiceReadyListener(serviceReadyListener);
            serviceReadyListener = null;
        }
    }

    private void startConversionNow(@NonNull NovelRepository.Novel novel) {
        conversionViewModel.start(this, novel);
        showProgressDialog();
    }

    private void showProgressDialog() {
        if (progressDialog != null) {
            return;
        }
        String name = conversionViewModel.getNovelName();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_conversion_progress, null);
        conversionProgress = view.findViewById(R.id.conversionProgress);
        conversionPercent = view.findViewById(R.id.conversionPercent);
        Integer current = conversionViewModel.getProgress().getValue();
        int progress = current == null ? 1 : current;
        conversionProgress.setProgress(progress);
        conversionPercent.setText(getString(R.string.progress_percent, progress));
        updateProgressAccessibility(progress);
        progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.convert_start_title,
                        name == null ? "" : name))
                .setIcon(R.drawable.icon)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> conversionViewModel.cancel())
                .create();
        progressDialog.show();
    }

    private void onConversionProgress(Integer percent) {
        if (percent == null) {
            return;
        }
        if (conversionViewModel.isRunning() && progressDialog == null) {
            showProgressDialog();
        }
        if (conversionProgress != null) {
            conversionProgress.setProgress(percent);
            updateProgressAccessibility(percent);
        }
        if (conversionPercent != null) {
            conversionPercent.setText(getString(R.string.progress_percent, percent));
        }
    }

    private void updateProgressAccessibility(int percent) {
        if (conversionProgress != null) {
            conversionProgress.setContentDescription(
                    getString(R.string.a11y_progress, percent));
        }
        if (conversionPercent != null
                && (percent == 25 || percent == 50 || percent == 75 || percent == 100)) {
            conversionPercent.announceForAccessibility(
                    getString(R.string.a11y_progress, percent));
        }
    }

    private void onConversionSuccess(@NonNull NovelConverter.Result result) {
        dismissProgress();
        if (isFinishing() || isDestroyed()) {
            return;
        }
        String name = conversionViewModel.getNovelName();
        showConversionComplete(name == null ? "" : name, result);
    }

    private void onConversionFailure(@NonNull String message) {
        dismissProgress();
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        conversionProgress = null;
        conversionPercent = null;
    }

    private void showConversionComplete(String novelName, NovelConverter.Result result) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.convert_done_title, novelName))
                .setIcon(R.drawable.icon)
                .setMessage(getString(R.string.convert_done_message,
                        result.savedPath,
                        result.author,
                        result.chapterCount,
                        String.format(Locale.ROOT, "%.1f", result.seconds)))
                .setPositiveButton(R.string.share,
                        (dialog, which) -> shareNovel(result.shareFile))
                .setNeutralButton(R.string.action_reward_author,
                        (dialog, which) -> Reward.showRewardDialog(this))
                .setNegativeButton(R.string.action_feedback,
                        (dialog, which) -> LinkUtils.open(this, getString(R.string.feedbackUrl)))
                .show();
    }

    private void shareNovel(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            NovelRepository.Novel novel = pendingConversionNovel;
            pendingConversionNovel = null;
            if (novel != null) {
                beginConversion(novel);
            }
            return;
        }
        if (requestCode != ReadingEnvironmentChecker.REQUEST_CODE_STORAGE_PERMISSION) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (granted && ReadingEnvironmentChecker.canReadNovels(this)) {
            refreshNovels();
        } else if (!granted) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.storage_permission_denied_title)
                    .setMessage(R.string.storage_permission_message)
                    .setIcon(R.drawable.icon)
                    .setPositiveButton(R.string.go_settings, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(environmentRetry);
        mainHandler.removeCallbacks(serviceWaitTimeout);
        removeServiceReadyListener();
        dismissProgress();
        if (loadingToast != null) {
            loadingToast.cancel();
            loadingToast = null;
        }
        super.onDestroy();
    }
}
