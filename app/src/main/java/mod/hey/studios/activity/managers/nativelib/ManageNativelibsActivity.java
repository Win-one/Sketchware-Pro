package mod.hey.studios.activity.managers.nativelib;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.oneskyer.library.model.DialogConfigs;
import com.oneskyer.library.model.DialogProperties;
import com.oneskyer.library.view.FilePickerDialog;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.databinding.DialogInputLayoutBinding;
import pro.sketchware.databinding.ManageFileBinding;
import pro.sketchware.databinding.ManageJavaItemHsBinding;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import mod.hey.studios.util.Helper;
import mod.jbk.util.AddMarginOnApplyWindowInsetsListener;

public class ManageNativelibsActivity extends BaseAppCompatActivity implements View.OnClickListener {
    private FilePickerDialog filePicker;
    private FilePathUtil fpu;
    private FileResConfig frc;
    private String numProj;
    private String nativeLibrariesPath;

    private ManageFileBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ManageFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("sc_id")) numProj = getIntent().getStringExtra("sc_id");

        frc = new FileResConfig(numProj);
        fpu = new FilePathUtil();

        setupDialog();
        checkDir();
        setupUI();
    }


    private void checkDir() {
        if (FileUtil.isExistFile(fpu.getPathNativelibs(numProj))) {
            nativeLibrariesPath = fpu.getPathNativelibs(numProj);
            handleAdapter(nativeLibrariesPath);
            handleFab();
            return;
        }
        FileUtil.makeDir(fpu.getPathNativelibs(numProj));
        FileUtil.makeDir(fpu.getPathNativelibs(numProj) + "/armeabi");
        FileUtil.makeDir(fpu.getPathNativelibs(numProj) + "/armeabi-v7a");
        FileUtil.makeDir(fpu.getPathNativelibs(numProj) + "/arm64-v8a");
        FileUtil.makeDir(fpu.getPathNativelibs(numProj) + "/x86");

        checkDir();
    }

    private void handleAdapter(String str) {
        ArrayList<String> nativelibsFile = frc.getNativelibsFile(str);
        Helper.sortPaths(nativelibsFile);
        CustomAdapter adapter = new CustomAdapter(nativelibsFile);
        binding.filesListRecyclerView.setAdapter(adapter);

        binding.noContentLayout.setVisibility(nativelibsFile.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isInMainDirectory() {
        return nativeLibrariesPath.equals(fpu.getPathNativelibs(numProj));
    }

    private void handleFab() {
        if (isInMainDirectory()) {
            binding.showOptionsButton.setText(R.string.new_folder);
        } else {
            binding.showOptionsButton.setText(getString(R.string.import_library));
        }
    }

    private void setupUI() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.topAppBar.setTitle(R.string.native_library_manager);

        binding.showOptionsButton.setOnClickListener(view -> hideShowOptionsButton(false));
        binding.closeButton.setOnClickListener(view -> hideShowOptionsButton(true));
        binding.createNewButton.setOnClickListener(v -> {
            createNewDialog();
            hideShowOptionsButton(true);
        });
        binding.showOptionsButton.setOnClickListener(v -> {
            if (isInMainDirectory()) {
                createNewDialog();
            } else {
                filePicker.show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.createNewButton,
                new AddMarginOnApplyWindowInsetsListener(WindowInsetsCompat.Type.navigationBars(), WindowInsetsCompat.CONSUMED));
    }

    private void hideShowOptionsButton(boolean isHide) {
        binding.optionsLayout.animate()
                .translationY(isHide ? 300 : 0)
                .alpha(isHide ? 0 : 1)
                .setInterpolator(new OvershootInterpolator());

        binding.showOptionsButton.animate()
                .translationY(isHide ? 0 : 300)
                .alpha(isHide ? 1 : 0)
                .setInterpolator(new OvershootInterpolator());
    }

    @Override
    public void onBackPressed() {
        nativeLibrariesPath = nativeLibrariesPath.substring(0, nativeLibrariesPath.lastIndexOf("/"));
        if (nativeLibrariesPath.contains("native_libs")) {
            handleAdapter(nativeLibrariesPath);
            handleFab();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == binding.showOptionsButton) {
            createNewDialog();
        }
    }

    private void createNewDialog() {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;
        var textInputLayout = dialogBinding.textInputLayout;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.create_a_new_folder)
                .setMessage(R.string.enter_the_name_of_the_new_folder)
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.common_word_create, null)
                .create();

        dialogBinding.chipGroupTypes.setVisibility(View.GONE);
        textInputLayout.setHint(getString(R.string.folder_name));

        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String name = inputText.getText().toString();

                if (name.isEmpty()) {
                    textInputLayout.setError(getString(R.string.invalid_folder_name));
                    return;
                }
                textInputLayout.setError(null);

                String path = fpu.getPathNativelibs(numProj) + "/" + name;

                if (FileUtil.isExistFile(path)) {
                    textInputLayout.setError(getString(R.string.folder_already_exists));
                    return;
                }
                textInputLayout.setError(null);

                FileUtil.makeDir(path);
                handleAdapter(nativeLibrariesPath);
                SketchwareUtil.toast(getString(R.string.created_folder_successfully));

                dialog.dismiss();
            });

            dialog.setView(dialogBinding.getRoot());
            dialog.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputText.requestFocus();
        });

        dialog.show();
    }


    private void setupDialog() {
        File externalStorageDir = new File(FileUtil.getExternalStorageDir());

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = externalStorageDir;
        properties.error_dir = externalStorageDir;
        properties.offset = externalStorageDir;
        properties.extensions = new String[]{"so"};

        filePicker = new FilePickerDialog(this, properties);
        filePicker.setTitle(R.string.select_a_native_library);
        filePicker.setDialogSelectionListener(selections -> {
            for (String path : selections) {
                try {
                    FileUtil.copyDirectory(new File(path), new File(nativeLibrariesPath + File.separator + Uri.parse(path).getLastPathSegment()));
                } catch (IOException e) {
                    SketchwareUtil.toastError(getString(R.string.couldn_t_import_library) + e.getMessage() + "]");
                }
            }

            handleAdapter(nativeLibrariesPath);
            handleFab();
        });
    }

    private void showRenameDialog(final String path) {
        DialogInputLayoutBinding dialogBinding = DialogInputLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.common_word_rename)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.common_word_rename, (dialogInterface, i) -> {
                    String newName = inputText.getText().toString();
                    if (!newName.isEmpty()) {
                        if (FileUtil.renameFile(path, path.substring(0, path.lastIndexOf(File.separator)) + File.separator + newName)) {
                            SketchwareUtil.toast(getString(R.string.renamed_successfully));
                        } else {
                            SketchwareUtil.toastError(getString(R.string.renaming_failed));
                        }
                        handleAdapter(nativeLibrariesPath);
                        handleFab();
                    } else {
                        SketchwareUtil.toast(getString(R.string.nothing_changed));
                    }
                    dialogInterface.dismiss();
                })
                .create();

        try {
            inputText.setText(path.substring(path.lastIndexOf("/") + 1));
        } catch (Exception ignored) {
        }

        dialog.setView(dialogBinding.getRoot());
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
        private final ArrayList<String> data;

        public CustomAdapter(ArrayList<String> arrayList) {
            data = arrayList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageJavaItemHsBinding binding = ManageJavaItemHsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = data.get(position);
            var binding = holder.binding;

            if (FileUtil.isDirectory(path)) {
                binding.more.setVisibility(View.GONE);
            } else {
                binding.more.setVisibility(View.VISIBLE);
            }

            binding.title.setText(Uri.parse(path).getLastPathSegment());
            binding.icon.setImageResource(FileUtil.isDirectory(path) ? R.drawable.ic_mtrl_folder : R.drawable.ic_mtrl_file);

            binding.more.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(ManageNativelibsActivity.this, v);
                menu.inflate(R.menu.popup_menu_double);

                menu.getMenu().getItem(0).setVisible(false);
                menu.getMenu().getItem(1).setVisible(false);
                menu.getMenu().getItem(2).setVisible(false);

                menu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.delete -> {
                            FileUtil.deleteFile(frc.listFileNativeLibs.get(position));
                            handleAdapter(nativeLibrariesPath);
                        }
                        case R.id.rename -> showRenameDialog(frc.listFileNativeLibs.get(position));
                        default -> {
                            return false;
                        }
                    }

                    return true;
                });
                menu.show();
            });

            binding.getRoot().setOnLongClickListener((View.OnLongClickListener) view -> {
                if (FileUtil.isDirectory(frc.listFileNativeLibs.get(position))) {
                    PopupMenu menu = new PopupMenu(ManageNativelibsActivity.this, view);
                    menu.getMenu().add(R.string.common_word_delete);
                    menu.setOnMenuItemClickListener(item -> {
                        FileUtil.deleteFile(frc.listFileNativeLibs.get(position));
                        handleAdapter(nativeLibrariesPath);
                        SketchwareUtil.toast(getString(R.string.common_word_deleted));

                        return true;
                    });
                    menu.show();
                }
                return true;
            });

            binding.getRoot().setOnClickListener(v -> {
                if (FileUtil.isDirectory(frc.listFileNativeLibs.get(position))) {
                    nativeLibrariesPath = frc.listFileNativeLibs.get(position);
                    handleAdapter(nativeLibrariesPath);
                    handleFab();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ManageJavaItemHsBinding binding;

            public ViewHolder(ManageJavaItemHsBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
