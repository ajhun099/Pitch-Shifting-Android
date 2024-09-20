package com.example.pitchshifting;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.example.pitchshifting.model.FileSize;
import com.example.pitchshifting.processor.AudioPlayer;
import com.example.pitchshifting.processor.AudioRecorder;
import com.example.pitchshifting.processor.PitchShiftFileSaver;
import com.example.pitchshifting.processor.PitchShiftPlayer;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class MainActivity extends AppCompatActivity {

    private TarsosDSPAudioFormat _format;
    private PitchShiftPlayer _shifPlayer;
    private PitchShiftFileSaver _shiftFileSaver;
    private AudioRecorder _recorder;
    private AudioPlayer _player;

    private Button _buttonRecord;
    private Button _buttonPlay;
    private Button _buttonSave;
    private TextView _labelFileSizeBefore;
    private TextView _labelFileSizeAfter;
    private TextView _labelFileLocation;
    private TextView _labelTimer;

    private final String _labelAfter = "Ukuran file setelah pitch shifting: ";

    private boolean _isPlaying = false;
    private boolean _isRecording = false;
    private boolean _pitchShiftEnabled = false;

    private final String _recordFileName = "recorded_sound.wav";

    private String _dataPath;
    private File _recordedFile;
    private File _pitchFile;

    int _sampleRate;
    private int _size = 1024;
    private float _factor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _dataPath = getExternalFilesDir(null).getAbsolutePath();

        Button _buttonPickFile = findViewById(R.id.buttonPickFile);
        CheckBox _checkboxPitchShiftState = findViewById(R.id.checkboxPitchShiftState);
        Slider _sliderFactor = findViewById(R.id.sliderFactor);

        _buttonPickFile.setOnClickListener(view -> openFileChooser());
        _checkboxPitchShiftState.setOnCheckedChangeListener((compoundButton, b) -> _pitchShiftEnabled = b);
        _sliderFactor.addOnChangeListener((slider, value, fromUser) -> {
            _factor = value;

            if (_shifPlayer != null) {
                _shifPlayer.setFactor(1.0f / _factor);
            }
        });

        _buttonRecord = findViewById(R.id.recordButton);
        _buttonPlay = findViewById(R.id.buttonPlay);
        _buttonSave = findViewById(R.id.buttonSave);
        _labelFileSizeBefore = findViewById(R.id.labelFileSizeBefore);
        _labelFileSizeAfter = findViewById(R.id.labelFileSizeAfter);
        _labelFileLocation = findViewById(R.id.labelFileLocation);
        _labelTimer = findViewById(R.id.labelTimer);

        _buttonRecord.setOnClickListener(view -> recordSound());
        _buttonPlay.setOnClickListener(view -> playSound());
        _buttonSave.setOnClickListener(this::savePitchShiftedFile);

        generateFormat();
    }

    protected void openFileChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionX.init(this)
                    .permissions(Manifest.permission.READ_MEDIA_AUDIO)
                    .onForwardToSettings ((scope, deniedList) -> scope.showForwardToSettingsDialog(deniedList, "Kamu setidaknya harus memberikan izin berikut", "OK", "Cancel"))
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("audio/*");

                            fileChooserLauncher.launch(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Tidak mendapatkan izin", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            PermissionX.init(this)
                    .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .onForwardToSettings ((scope, deniedList) -> scope.showForwardToSettingsDialog(deniedList, "Kamu setidaknya harus memberikan izin berikut", "OK", "Cancel"))
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("audio/*");

                            fileChooserLauncher.launch(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Tidak mendapatkan izin", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    protected ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();

            if (intent == null) {
                return;
            }

            Uri uri = intent.getData();

            String path = Uri2Path(this, uri);
            String extension = getExtensionByStringHandling(path).get();

            _labelFileLocation.setText(path);
            _recordedFile = new File(path);

            if (extension.equals("mp3")) {
                String outputPath = _dataPath + '/' + _recordFileName;
                FFmpegKit.execute("-y -i \"" + _recordedFile.getAbsolutePath() + "\" -acodec pcm_s16le -ar 44100 -ac 1 \"" + outputPath + "\"");
                _recordedFile = new File(_dataPath, _recordFileName);
            }

            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            try {
                metaRetriever.setDataSource(_recordedFile.getAbsolutePath());
                int sampleRate = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE));
                setSampleRate(sampleRate);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (_recordedFile.exists()) {
                _pitchFile = null;
                _labelFileSizeAfter.setText(_labelAfter);
                changeLabelBeforeText(_recordedFile);
            }
        }
    });

    public static String Uri2Path(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        if(ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }
        else if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String authority = uri.getAuthority();

            if(authority.startsWith("com.android.externalstorage")) {
                return Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
            }
            else {
                String idStr = "";
                if(authority.equals("media")) {
                    idStr = uri.toString().substring(uri.toString().lastIndexOf('/') + 1);
                }
                else if(authority.startsWith("com.android.providers")) {
                    idStr = DocumentsContract.getDocumentId(uri).split(":")[1];
                }

                ContentResolver contentResolver = context.getContentResolver();
                Cursor cursor = contentResolver.query(MediaStore.Files.getContentUri("external"),
                        new String[] {MediaStore.Files.FileColumns.DATA},
                        "_id=?",
                        new String[]{idStr}, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    try {
                        int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

                        return cursor.getString(idx);
                    } catch (Exception e) {
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        return null;
    }

    private Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private void setSampleRate(int sampleRate) {
        _sampleRate = sampleRate;

        switch (_sampleRate) {
            case 8000:
            case 11025:
            case 16000:
                _size = 1024;
                break;
            case 22050:
                _size = 4096;
                break;
            case 44100:
                _size = 8192;
                break;
            default:
                break;
        }

        generateFormat();
    }

    private void generateFormat() {
        _format = new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                _sampleRate,
                2 * 8,
                1,
                2 * 1,
                _sampleRate,
                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));
    }

    protected void recordSound() {
        PermissionX.init(this)
                .permissions(Manifest.permission.RECORD_AUDIO)
                .onForwardToSettings ((scope, deniedList) ->{
                    scope.showForwardToSettingsDialog(deniedList, "Kamu setidaknya harus memberikan izin berikut", "OK", "Cancel");
                })
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        if (_isRecording) {
                            stopRecording();
                            _isRecording = false;
                            _buttonRecord.setText("MULAI REKAM");
                        } else {
                            startRecording();
                            _isRecording = true;
                            _buttonRecord.setText("HENTIKAN REKAM");
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Tidak mendapatkan izin", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public String getFileSize(String filename) {
        File file = new File(filename);

        try {
            FileSize fileSize = new FileSize(file);

            return fileSize.getString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            return "File not found";
        }
    }

    private void changeLabelBeforeText(File file) {
        String _labelBefore = "Ukuran file sebelum pitch shifting: ";
        _labelFileSizeBefore.setText(_labelBefore + getFileSize(file.getAbsolutePath()));
    }

    private void changeLabelAfterText(File file) {
        _labelFileSizeAfter.setText(_labelAfter + getFileSize(file.getAbsolutePath()));
    }

    long startTime = 0;
    Timer timer = new Timer();

    class task extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                if (seconds >= 1) {
                    recordSound();
                }

                _labelTimer.setText(String.format("%d:%02d", minutes, seconds));
            });
        }
    }

    protected void startRecording() {
        startTime = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new task(), 0, 1000);

        releaseDispatcher();
        setSampleRate(8000);
        _recordedFile = new File(_dataPath, _recordFileName);

        try {
            _recorder = new AudioRecorder(_recordedFile, _format, _sampleRate, _size);
            _recorder.setOnProcessingFinishedHandler(() -> {
                runOnUiThread(() -> {
                    if (_recordedFile.exists()) {
                        changeLabelBeforeText(_recordedFile);
                    }
                });
            });
            _recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void stopRecording() {
        timer.cancel();
        timer.purge();

        if (_recorder != null) {
            _recorder.stop();
        }
    }

    private void playSound() {
        if (_isPlaying) {
            stopPlaying();
            _isPlaying = false;
            _buttonPlay.setText("PUTAR AUDIO");
        } else {
            if (_recordedFile == null) return;
            if (!_recordedFile.exists()) return;

            if (_pitchShiftEnabled) {
                startPitchShifterPlayer();
            } else {
                startPlaying();
            }
            _isPlaying = true;
            _buttonPlay.setText("HENTIKAN AUDIO");
        }
    }

    private void startPlaying() {
        releaseDispatcher();

        try {
            _player = new AudioPlayer(_recordedFile, _format, _sampleRate, _size);
            _player.setOnProcessingFinishedHandler(() -> {
                playSound();
            });
            _player.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        releaseDispatcher();
    }

    private int getOverlap() {
        return (int) (_size * 87.5 / 100);
    }

    private void startPitchShifterPlayer() {
        try {
            _shifPlayer = new PitchShiftPlayer(_recordedFile, _format, _factor, _size, _sampleRate, getOverlap());
            _shifPlayer.setOnProcessingFinishedHandler(() -> {
                playSound();
            });
            _shifPlayer.play();
        } catch (Exception e) {
            playSound();
            e.printStackTrace();
        }
    }

    private void savePitchShiftedFile(View view) {
        if (_recordedFile == null) {
            Snackbar snackbar = Snackbar
                    .make(view, "Harap pilih file terlebih dahulu", Snackbar.LENGTH_LONG);

            snackbar.show();
            return;
        }
        if (!_recordedFile.exists()) {
            Snackbar snackbar = Snackbar
                    .make(view, "Harap pilih file terlebih dahulu", Snackbar.LENGTH_LONG);

            snackbar.show();
            return;
        }
        if (!checkPermission()) {
            requestWritePermission();

            if (!checkPermission()) {
                Toast.makeText(getApplicationContext(), "Tidak mendapatkan izin", Toast.LENGTH_SHORT).show();
            }
        }

        createFolder();

        try {
            _buttonSave.setText("Menyimpan...");
            _buttonSave.setEnabled(false);

            _shiftFileSaver = new PitchShiftFileSaver(_recordedFile, _factor, _format, _sampleRate, _size, getOverlap());
            _shiftFileSaver.setOnProcessingFinishedHandler(() -> {
                runOnUiThread(() -> {
                    _buttonSave.setText("SIMPAN FILE");
                    _buttonSave.setEnabled(true);

                    if (_pitchFile.exists()) {
                        changeLabelAfterText(_pitchFile);
                        _labelFileLocation.setText("File disimpan di: " + _pitchFile.getAbsolutePath());
                    }

                    Snackbar snackbar = Snackbar
                            .make(view, "File telah disimpan", Snackbar.LENGTH_LONG);

                    snackbar.show();
                });
            });
            int num = 0;
            String _pitchFileName = "pitch_sound.wav";
            _pitchFile = new File(Environment.getExternalStorageDirectory() + "/Pitch Shifting", _pitchFileName);
            while (_pitchFile.exists()) {
                num = num + 1;
                _pitchFile = new File(Environment.getExternalStorageDirectory() + "/Pitch Shifting", _pitchFileName.substring(0, _pitchFileName.length() - 4) + num + ".wav");
            }
            _shiftFileSaver.saveFile(_pitchFile);
        } catch (Exception e) {
            _buttonSave.setText("SIMPAN FILE");
            _buttonSave.setEnabled(true);

            e.printStackTrace();
        }
    }

    private void requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }
            catch (Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else {
            PermissionX.init(this)
                    .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .onForwardToSettings ((scope, deniedList) ->{
                        scope.showForwardToSettingsDialog(deniedList, "Kamu setidaknya harus memberikan izin berikut", "OK", "Cancel");
                    })
                    .request((allGranted, grantedList, deniedList) -> {

                    });
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            }
    );

    public boolean checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        }
        else{
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void createFolder(){
        String folderName = "Pitch Shifting";
        File file = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
        file.mkdir();
    }

    private void releaseDispatcher() {
        if (_recorder != null) {
            _recorder.stop();
        }

        if (_player != null) {
            _player.stop();
        }

        if (_shifPlayer != null) {
            _shifPlayer.stop();
        }

        if (_shiftFileSaver != null) {
            _shiftFileSaver.stop();
        }
    }

    @Override
    protected void onStop() {
        timer.cancel();
        timer.purge();
        releaseDispatcher();
        super.onStop();
    }
}