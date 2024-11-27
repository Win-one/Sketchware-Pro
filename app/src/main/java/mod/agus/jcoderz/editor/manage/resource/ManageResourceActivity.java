package mod.agus.jcoderz.editor.manage.resource;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.bobur.androidsvg.SVG;
import com.bumptech.glide.Glide;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import mod.bobur.StringEditorActivity;
import mod.bobur.XmlToSvgConverter;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.code.SrcCodeEditorLegacy;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.jbk.util.AddMarginOnApplyWindowInsetsListener;
import pro.sketchware.R;
import pro.sketchware.activities.coloreditor.ColorEditorActivity;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.databinding.DialogInputLayoutBinding;
import pro.sketchware.databinding.ManageFileBinding;
import pro.sketchware.databinding.ManageJavaItemHsBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

@SuppressLint("SetTextI18n")
public class ManageResourceActivity extends BaseAppCompatActivity {

    private CustomAdapter adapter;
    private FilePickerDialog dialog;
    private FilePathUtil fpu;
    private FileResConfig frc;
    private String numProj;
    private String temp;

    private ManageFileBinding binding;

    public static String getLastDirectory(String path) {
        // Get the index of the last occurrence of '/' character
        int lastSlashIndex = path.lastIndexOf('/');

        // Get the substring from the start to the lastSlashIndex
        String parentPath = path.substring(0, lastSlashIndex);

        // Get the index of the last occurrence of '/' in the parentPath
        lastSlashIndex = parentPath.lastIndexOf('/');

        // Extract the last directory name
        return parentPath.substring(lastSlashIndex + 1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ManageFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("sc_id")) {
            numProj = getIntent().getStringExtra("sc_id");
        }
        Helper.fixFileprovider();
        frc = new FileResConfig(numProj);
        fpu = new FilePathUtil();
        setupDialog();
        checkDir();
        initToolbar();
    }

    private void checkDir() {
        if (FileUtil.isExistFile(fpu.getPathResource(numProj))) {
            temp = fpu.getPathResource(numProj);
            handleAdapter(temp);
            handleFab();
            return;
        }
        FileUtil.makeDir(fpu.getPathResource(numProj));
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/anim");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/drawable");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/drawable-xhdpi");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/layout");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/menu");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/values");
        checkDir();
    }

    private void handleAdapter(String str) {
        ArrayList<String> resourceFile = frc.getResourceFile(str);
        //noinspection Java8ListSort
        Collections.sort(resourceFile, String.CASE_INSENSITIVE_ORDER);
        adapter = new CustomAdapter(resourceFile);
        binding.filesListRecyclerView.setAdapter(adapter);

        binding.noContentLayout.setVisibility(resourceFile.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isInMainDirectory() {
        return temp.equals(fpu.getPathResource(numProj));
    }

    private void handleFab() {
        var optionsButton = binding.showOptionsButton;
        if (isInMainDirectory()) {
            optionsButton.setText(R.string.create_new);
            hideShowOptionsButton(true);
        } else {
            optionsButton.setText(R.string.create_or_import);
        }
    }

    private void initToolbar() {
        binding.topAppBar.setTitle(R.string.text_title_menu_resource);
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.showOptionsButton.setOnClickListener(view -> {
            if (isInMainDirectory()) {
                createNewDialog(true);
                return;
            }
            hideShowOptionsButton(false);
        });
        binding.closeButton.setOnClickListener(view -> hideShowOptionsButton(true));
        binding.createNewButton.setOnClickListener(v -> {
            createNewDialog(isInMainDirectory());
            hideShowOptionsButton(true);
        });
        binding.importNewButton.setOnClickListener(v -> {
            dialog.show();
            hideShowOptionsButton(true);
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
    public void onResume() {
        super.onResume();
        binding.filesListRecyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        try {
            temp = temp.substring(0, temp.lastIndexOf("/"));
            if (temp.contains("resource")) {
                handleAdapter(temp);
                handleFab();
                return;
            }
        } catch (IndexOutOfBoundsException ignored) {
        }
        setResult(RESULT_OK);
        finish();
        super.onBackPressed();
    }

    private void createNewDialog(final boolean isFolder) {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(isFolder ? getString(R.string.create_a_new_folder):getString(R.string.create_a_new_file))
                .setMessage(getString(R.string.enter_a_name_for_the_new) + (isFolder ? getString(R.string.common_word_folder) : getString(R.string.common_word_file)))
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.common_word_create, null)
                .create();

        dialogBinding.chipGroupTypes.setVisibility(View.GONE);
        if (!isFolder) {
            dialogBinding.inputText.setText(".xml");
        }

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((androidx.appcompat.app.AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (inputText.getText().toString().isEmpty()) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.invalid_name));
                    return;
                }

                String name = inputText.getText().toString();
                String path;
                if (isFolder) {
                    path = fpu.getPathResource(numProj) + "/" + name;
                } else {
                    path = new File(temp + File.separator + name).getAbsolutePath();
                }

                if (FileUtil.isExistFile(path)) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.file_exists_already));
                    return;
                }
                if (isFolder) {
                    FileUtil.makeDir(path);
                } else {
                    FileUtil.writeFile(path, "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                }
                handleAdapter(temp);
                SketchwareUtil.toast(Helper.getResString(R.string.created_file_successfully));
                dialog.dismiss();
            });

            dialog.setView(dialogBinding.getRoot());
            dialog.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputText.requestFocus();

            if (!isFolder) {
                inputText.setSelection(0);
            }
        });

        dialog.show();
    }

    private void setupDialog() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_AND_DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = Environment.getExternalStorageDirectory();
        properties.offset = Environment.getExternalStorageDirectory();
        properties.extensions = null;
        dialog = new FilePickerDialog(this, properties, R.style.RoundedCornersDialog);
        dialog.setTitle(R.string.select_a_resource_file);
        dialog.setDialogSelectionListener(selections -> {
            for (String path : selections) {
                try {
                    FileUtil.copyDirectory(new File(path), new File(temp + File.separator + Uri.parse(path).getLastPathSegment()));
                } catch (IOException e) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.couldn_t_import_resource) + e.getMessage() + "]");
                }
            }
            handleAdapter(temp);
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
                    if (!inputText.getText().toString().isEmpty()) {
                        if (FileUtil.renameFile(path, path.substring(0, path.lastIndexOf("/")) + "/" + inputText.getText().toString())) {
                            SketchwareUtil.toast(Helper.getResString(R.string.renamed_successfully));
                        } else {
                            SketchwareUtil.toastError(Helper.getResString(R.string.renaming_failed));
                        }
                        handleAdapter(temp);
                        handleFab();
                    }
                    dialogInterface.dismiss();
                })
                .create();


        try {
            inputText.setText(path.substring(path.lastIndexOf("/") + 1));
        } catch (IndexOutOfBoundsException e) {
            inputText.setText(path);
        }

        dialog.setView(dialogBinding.getRoot());
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showDeleteDialog(final int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.common_word_delete + Uri.fromFile(new File(adapter.getItem(position))).getLastPathSegment() + "?")
                .setMessage(R.string.are_you_sure_you_want_to_delete_this+ (FileUtil.isDirectory(adapter.getItem(position)) ? getString(R.string.common_word_folder) : getString(R.string.common_word_file)) + "? "
                        + getString(R.string.this_action_cannot_be_undone))
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                    FileUtil.deleteFile(frc.listFileResource.get(position));
                    handleAdapter(temp);
                    SketchwareUtil.toast(Helper.getResString(R.string.common_word_deleted));
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show();
    }

    private void goEdit(int position) {
        if (frc.listFileResource.get(position).endsWith("strings.xml")) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), StringEditorActivity.class);
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            intent.putExtra("xml", "");
            startActivity(intent);
        } else if (frc.listFileResource.get(position).endsWith("colors.xml")) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ColorEditorActivity.class);
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            startActivity(intent);
        } else if (frc.listFileResource.get(position).endsWith("xml")) {
            Intent intent = new Intent();
            if (ConfigActivity.isLegacyCeEnabled()) {
                intent.setClass(getApplicationContext(), SrcCodeEditorLegacy.class);
            } else {
                intent.setClass(getApplicationContext(), SrcCodeEditor.class);
            }
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            intent.putExtra("xml", "");
            startActivity(intent);
        } else {
            SketchwareUtil.toast(Helper.getResString(R.string.only_xml_files_can_be_edited));
        }
    }

    private void goEdit2(int position) {
        if (frc.listFileResource.get(position).endsWith("xml")) {
            Intent intent = new Intent();
            if (ConfigActivity.isLegacyCeEnabled()) {
                intent.setClass(getApplicationContext(), SrcCodeEditorLegacy.class);
            } else {
                intent.setClass(getApplicationContext(), SrcCodeEditor.class);
            }
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            intent.putExtra("xml", "");
            startActivity(intent);
        } else {
            SketchwareUtil.toast(Helper.getResString(R.string.only_xml_files_can_be_edited));
        }
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

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = data.get(position);
            var binding = holder.binding;

            binding.more.setVisibility(FileUtil.isDirectory(path) ? View.GONE : View.VISIBLE);
            binding.title.setText(Uri.parse(path).getLastPathSegment());

            if (FileUtil.isDirectory(path)) {
                binding.icon.setImageResource(R.drawable.ic_mtrl_folder);
            } else {
                try {
                    if (FileUtil.isImageFile(path)) {
                        Glide.with(ManageResourceActivity.this).load(new File(path)).into(binding.icon);
                    } else if (path.endsWith(".xml") && "drawable".equals(getLastDirectory(path))) {
                        SVG svg = SVG.getFromString(XmlToSvgConverter.xml2svg(FileUtil.readFile(path)));
                        Picture picture = svg.renderToPicture();
                        binding.icon.setImageDrawable(new PictureDrawable(picture));
                    } else {
                        binding.icon.setImageResource(R.drawable.ic_mtrl_file);
                    }
                } catch (Exception ignored) {
                    binding.icon.setImageResource(R.drawable.ic_mtrl_file);
                }
            }

            binding.more.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(ManageResourceActivity.this, v);
                popupMenu.inflate(R.menu.popup_menu_double);
                popupMenu.getMenu().getItem(0).setVisible(false);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.edit_with -> {
                            if (frc.listFileResource.get(position).endsWith("xml")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(new File(frc.listFileResource.get(position))), "text/plain");
                                startActivity(intent);
                            } else {
                                SketchwareUtil.toast(Helper.getResString(R.string.only_xml_files_can_be_edited));
                            }
                        }
                        case R.id.edit -> goEdit2(position);
                        case R.id.delete -> showDeleteDialog(position);
                        case R.id.rename -> showRenameDialog(frc.listFileResource.get(position));
                        default -> {
                            return false;
                        }
                    }
                    return true;
                });
                popupMenu.show();
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (FileUtil.isDirectory(frc.listFileResource.get(position))) {
                    PopupMenu popupMenu = new PopupMenu(ManageResourceActivity.this, binding.more);
                    popupMenu.getMenu().add(0,0,0,R.string.common_word_delete);
                    popupMenu.setOnMenuItemClickListener(item -> {
                        showDeleteDialog(position);
                        return true;
                    });
                    popupMenu.show();
                } else {
                    binding.more.performClick();
                }
                return true;
            });
            binding.getRoot().setOnClickListener(view -> {
                if (FileUtil.isDirectory(frc.listFileResource.get(position))) {
                    temp = frc.listFileResource.get(position);
                    handleAdapter(temp);
                    handleFab();
                    return;
                }
                goEdit(position);
            });
        }

        public String getItem(int position) {
            return data.get(position);
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
