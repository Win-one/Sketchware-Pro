package mod.hilal.saif.activities.android_manifest;

import static pro.sketchware.utility.SketchwareUtil.getDip;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import mod.remaker.util.ThemeUtils;
import pro.sketchware.R;
import pro.sketchware.databinding.CustomDialogAttributeBinding;
import pro.sketchware.databinding.ActivityManageCustomAttributeBinding;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.utility.FileUtil;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.android_manifest.ActComponentsDialog;
import mod.remaker.view.CustomAttributeView;

public class AndroidManifestInjectionDetails extends BaseAppCompatActivity {

    private static String ATTRIBUTES_FILE_PATH;
    private final ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
    private String src_id;
    private String activityName;
    private String type;
    private String constant;

    private ActivityManageCustomAttributeBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCustomAttributeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("sc_id") && getIntent().hasExtra("file_name") && getIntent().hasExtra("type")) {
            src_id = getIntent().getStringExtra("sc_id");
            activityName = getIntent().getStringExtra("file_name").replaceAll(".java", "");
            type = getIntent().getStringExtra("type");
        }
        ATTRIBUTES_FILE_PATH = FileUtil.getExternalStorageDir().concat("/.sketchware/data/").concat(src_id).concat("/Injection/androidmanifest/attributes.json");
        setupConst();
        setToolbar();
        setupViews();
    }

    private void setupConst() {
        switch (type) {
            case "all":
                constant = "_apply_for_all_activities";
                break;

            case "application":
                constant = "_application_attrs";
                break;

            case "permission":
                constant = "_application_permissions";
                break;

            default:
                constant = activityName;
                break;
        }
    }

    private void setupViews() {
        binding.addAttrFab.setOnClickListener(v -> showAddDial());
        refreshList();
    }

    private void refreshList() {
        listMap.clear();
        ArrayList<HashMap<String, Object>> data;
        if (FileUtil.isExistFile(ATTRIBUTES_FILE_PATH)) {
            data = new Gson().fromJson(FileUtil.readFile(ATTRIBUTES_FILE_PATH), Helper.TYPE_MAP_LIST);
            for (HashMap<String, Object> item : data) {
                String str = (String) item.get("name");
                if (str.equals(constant)) {
                    listMap.add(item);
                }
            }
            binding.addAttrListview.setAdapter(new ListAdapter(listMap));
            ((BaseAdapter) binding.addAttrListview.getAdapter()).notifyDataSetChanged();
        }
    }

    private void setToolbar() {
        String activity = getString(R.string.attributes_for_all_activities);
        String attributes = getString(R.string.application_attributes);
        String permissions = getString(R.string.application_permissions);
        String str = switch (type) {
            case "all" -> activity;
            case "application" -> attributes;
            case "permission" -> permissions;
            default -> activityName;
        };
        binding.toolbar.setTitle(str);

        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        if (!str.equals(activity) && !str.equals(attributes) && !str.equals(permissions)) {
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.asd_components) {
                    ActComponentsDialog acd = new ActComponentsDialog(this, src_id, activityName);
                    acd.show();
                    return true;
                }
                return false;
            });
        } else {
            binding.toolbar.getMenu().clear();
        }
    }

    private void showDial(int pos) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.edit_value);
        DialogCreateNewFileLayoutBinding attributeBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        attributeBinding.chipGroupTypes.setVisibility(View.GONE);
        dialog.setView(attributeBinding.getRoot());

        attributeBinding.inputText.setText((String) listMap.get(pos).get("value"));
        attributeBinding.inputText.setHint("android:attr=\"value\"");
        dialog.setPositiveButton(R.string.common_word_save, (dialog1, which) -> {
            listMap.get(pos).put("value", attributeBinding.inputText.getText().toString());
            applyChange();
        });

        dialog.show();
    }

    private void showAddDial() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(type.equals("permission") ? getString(R.string.add_new_permission) : getString(R.string.add_new_attribute));
        CustomDialogAttributeBinding attributeBinding = CustomDialogAttributeBinding.inflate(getLayoutInflater());
        dialog.setView(attributeBinding.getRoot());
        if (type.equals("permission")) {
            attributeBinding.inputRes.setText("android");
            attributeBinding.inputAttr.setText("name");
            attributeBinding.inputLayoutValue.setHint("permission");
        }
        dialog.setPositiveButton(R.string.common_word_save, (dialog1, which) -> {
            String fstr = attributeBinding.inputRes.getText().toString().trim() + ":" + attributeBinding.inputAttr.getText().toString().trim() + "=\"" + attributeBinding.inputValue.getText().toString().trim() + "\"";
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", constant);
            map.put("value", fstr);
            listMap.add(map);
            applyChange();
            dialog1.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, (dialog1, which) -> dialog1.dismiss());
        dialog.show();
    }

    private void applyChange() {
        ArrayList<HashMap<String, Object>> data;
        if (FileUtil.isExistFile(ATTRIBUTES_FILE_PATH)) {
            data = new Gson().fromJson(FileUtil.readFile(ATTRIBUTES_FILE_PATH), Helper.TYPE_MAP_LIST);
            for (int i = data.size() - 1; i > -1; i--) {
                String str = (String) data.get(i).get("name");
                if (str.equals(constant)) {
                    data.remove(i);
                }
            }
            data.addAll(listMap);
        } else {
            data = new ArrayList<>(listMap);
        }
        FileUtil.writeFile(ATTRIBUTES_FILE_PATH, new Gson().toJson(data));
        refreshList();
    }

    private TextView newText(String str, float size, int color, int width, int height, float weight) {
        TextView temp_card = new TextView(this);
        temp_card.setLayoutParams(new LinearLayout.LayoutParams(width, height, weight));
        temp_card.setPadding((int) getDip(4), (int) getDip(4), (int) getDip(4), (int) getDip(4));
        temp_card.setTextColor(color);
        temp_card.setText(str);
        temp_card.setTextSize(size);
        return temp_card;
    }

    private class ListAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, Object>> _data;

        public ListAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public HashMap<String, Object> getItem(int position) {
            return _data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CustomAttributeView attributeView = new CustomAttributeView(parent.getContext());

            try {
                int violet = ThemeUtils.getColor(attributeView, R.attr.colorViolet);
                int onSurface = ThemeUtils.getColor(attributeView, com.google.android.material.R.attr.colorOnSurface);
                int green = ThemeUtils.getColor(attributeView, R.attr.colorGreen);

                SpannableString spannableString = new SpannableString((String) _data.get(position).get("value"));
                spannableString.setSpan(new ForegroundColorSpan(violet), 0, ((String) _data.get(position).get("value")).indexOf(":"), 33);
                spannableString.setSpan(new ForegroundColorSpan(onSurface), ((String) _data.get(position).get("value")).indexOf(":"), ((String) _data.get(position).get("value")).indexOf("=") + 1, 33);
                spannableString.setSpan(new ForegroundColorSpan(green), ((String) _data.get(position).get("value")).indexOf("\""), ((String) _data.get(position).get("value")).length(), 33);
                attributeView.getTextView().setText(spannableString);
            } catch (Exception e) {
                attributeView.getTextView().setText((String) _data.get(position).get("value"));
            }

            attributeView.getImageView().setVisibility(View.GONE);
            attributeView.setOnClickListener(v -> showDial(position));
            attributeView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(AndroidManifestInjectionDetails.this)
                        .setTitle(R.string.delete_this_attribute)
                        .setMessage(getString(R.string.this_action_cannot_be_undone))
                        .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                            listMap.remove(position);
                            applyChange();
                        })
                        .setNegativeButton(R.string.common_word_cancel, null)
                        .show();

                return true;
            });

            return attributeView;
        }
    }
}