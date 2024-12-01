package mod.hilal.saif.activities.tools;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import a.a.a.Zx;
import a.a.a.aB;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.lib.PCP;
import pro.sketchware.R;
import pro.sketchware.databinding.BlocksManagerBinding;
import pro.sketchware.databinding.DialogBlockConfigurationBinding;
import pro.sketchware.databinding.DialogPaletteBinding;
import pro.sketchware.lib.base.BaseTextWatcher;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class BlocksManager extends BaseAppCompatActivity {

    private ArrayList<HashMap<String, Object>> all_blocks_list = new ArrayList<>();
    private String blocks_dir = "";
    private String pallet_dir = "";
    private ArrayList<HashMap<String, Object>> pallet_listmap = new ArrayList<>();
    private BlocksManagerBinding binding;


    @Override
    public void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = BlocksManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        initializeLogic();
    }

    @Override
    public void onStop() {
        super.onStop();

        BlockLoader.refresh();
    }

    private void initialize() {
        binding.toolbar.setNavigationOnClickListener(v ->getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.block_menus) {
                showBlockConfigurationDialog();
                return true;
            }
            return false;
        });
        binding.fab.setOnClickListener(v -> showPaletteDialog(false, null, null, null, null));
    }

    private void initializeLogic() {
        readSettings();
        refresh_list();
        _recycleBin(binding.recycleBinCard);
    }

    @Override
    public void onResume() {
        super.onResume();

        readSettings();
        refresh_list();
    }

    private void showBlockConfigurationDialog() {
        aB dialog = new aB(this);
        dialog.a(R.drawable.services_48);
        dialog.b(getString(R.string.block_configuration));

        DialogBlockConfigurationBinding dialogBinding = DialogBlockConfigurationBinding.inflate(getLayoutInflater());

        dialogBinding.palettesPath.setText(pallet_dir.replace(FileUtil.getExternalStorageDir(), ""));
        dialogBinding.blocksPath.setText(blocks_dir.replace(FileUtil.getExternalStorageDir(), ""));

        dialog.a(dialogBinding.getRoot());

        dialog.b(Helper.getResString(R.string.common_word_save), view -> {
            ConfigActivity.setSetting(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                    dialogBinding.palettesPath.getText().toString());
            ConfigActivity.setSetting(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH,
                    dialogBinding.blocksPath.getText().toString());

            readSettings();
            refresh_list();
            dialog.dismiss();
        });

        dialog.a(Helper.getResString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));

        dialog.configureDefaultButton(getString(R.string.common_word_default), view -> {
            ConfigActivity.setSetting(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                    ConfigActivity.getDefaultValue(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH));
            ConfigActivity.setSetting(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH,
                    ConfigActivity.getDefaultValue(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH));

            readSettings();
            refresh_list();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void readSettings() {
        pallet_dir = FileUtil.getExternalStorageDir() + ConfigActivity.getStringSettingValueOrSetAndGet(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                (String) ConfigActivity.getDefaultValue(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH));
        blocks_dir = FileUtil.getExternalStorageDir() + ConfigActivity.getStringSettingValueOrSetAndGet(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH,
                (String) ConfigActivity.getDefaultValue(ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH));
                
        if (FileUtil.isExistFile(blocks_dir) && isValidJson(FileUtil.readFile(blocks_dir))) {
            try {
                all_blocks_list = new Gson().fromJson(FileUtil.readFile(blocks_dir), Helper.TYPE_MAP_LIST);

                if (all_blocks_list != null) {
                    return;
                }
                // fall-through to shared handler
            } catch (JsonParseException e) {
                // fall-through to shared handler
            }

            SketchwareUtil.showFailedToParseJsonDialog(this, new File(blocks_dir), "Custom Blocks", v -> readSettings());
        }
    }
   
    private Boolean isValidJson(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() || element.isJsonArray();
        } catch (JsonSyntaxException e) {
            return false;  
        }
    }

    private void refresh_list() {
        parsePaletteJson:
        {
            String paletteJsonContent;
            if (FileUtil.isExistFile(pallet_dir) && !(paletteJsonContent = FileUtil.readFile(pallet_dir)).isEmpty()) {
                try {
                    pallet_listmap = new Gson().fromJson(paletteJsonContent, Helper.TYPE_MAP_LIST);

                    if (pallet_listmap != null) {
                        break parsePaletteJson;
                    }
                    // fall-through to shared handler
                } catch (JsonParseException e) {
                    // fall-through to shared handler
                }

                SketchwareUtil.showFailedToParseJsonDialog(this, new File(pallet_dir), "Custom Block Palettes", v -> refresh_list());
            }
            pallet_listmap = new ArrayList<>();
        }

        Parcelable savedState = binding.listPallete.onSaveInstanceState();
        binding.listPallete.setAdapter(new PaletteAdapter(pallet_listmap));
        ((BaseAdapter) binding.listPallete.getAdapter()).notifyDataSetChanged();
        binding.listPallete.onRestoreInstanceState(savedState);

        binding.recycleSub.setText(getString(R.string.blocks) + (long) (getN(-1)));
    }

    private double getN(final double _p) {
        int n = 0;
        if (all_blocks_list == null) return 0;

        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (all_blocks_list.get(i).get("palette").toString().equals(String.valueOf((long) (_p)))) {
                n++;
            }
        }
        return (n);
    }

    private void moveUp(final double _p) {
        if (_p > 0) {
            Collections.swap(pallet_listmap, (int) (_p), (int) (_p + -1));

            Parcelable savedState = binding.listPallete.onSaveInstanceState();
            FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
            _swapRelatedBlocks(_p + 9, _p + 8);
            readSettings();
            refresh_list();
            binding.listPallete.onRestoreInstanceState(savedState);
        }
    }

    private void _recycleBin(final View _v) {
        binding.recycleBinCard.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), BlocksManagerDetailsActivity.class);
            intent.putExtra("position", "-1");
            intent.putExtra("dirB", blocks_dir);
            intent.putExtra("dirP", pallet_dir);
            startActivity(intent);
        });
        binding.recycleBinCard.setOnLongClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.recycle_bin))
                    .setMessage(getString(R.string.are_you_sure_you_want_to_empty_the_recycle_bin) +
                            getString(R.string.blocks_inside_will_be_deleted_permanently_you_cannot_recover_them))
                    .setPositiveButton(getString(R.string.common_word_apply), (dialog, which) -> _emptyRecyclebin())
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .show();
            return true;
        });
    }

    private void moveDown(final double _p) {
        if (_p < (pallet_listmap.size() - 1)) {
            Collections.swap(pallet_listmap, (int) (_p), (int) (_p + 1));
            {
                Parcelable savedState = binding.listPallete.onSaveInstanceState();
                FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                _swapRelatedBlocks(_p + 9, _p + 10);
                readSettings();
                refresh_list();
                binding.listPallete.onRestoreInstanceState(savedState);
            }
        }
    }

    private void removeRelatedBlocks(final double _p) {
        List<Map<String, Object>> newBlocks = new LinkedList<>();
        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (!(Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == _p)) {
                if (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) > _p) {
                    HashMap<String, Object> m = all_blocks_list.get(i);
                    m.put("palette", String.valueOf((long) (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) - 1)));
                    newBlocks.add(m);
                } else {
                    newBlocks.add(all_blocks_list.get(i));
                }
            }
        }
        FileUtil.writeFile(blocks_dir, new Gson().toJson(newBlocks));
        readSettings();
        refresh_list();
    }

    private void _swapRelatedBlocks(final double _f, final double _s) {
        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == _f) {
                all_blocks_list.get(i).put("palette", "123456789");
            }
            if (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == _s) {
                all_blocks_list.get(i).put("palette", String.valueOf((long) (_f)));
            }
        }

        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == 123456789) {
                all_blocks_list.get(i).put("palette", String.valueOf((long) (_s)));
            }
        }
        FileUtil.writeFile(blocks_dir, new Gson().toJson(all_blocks_list));
        readSettings();
        refresh_list();
    }

    private void _insertBlocksAt(final double _p) {
        for (int i = 0; i < all_blocks_list.size(); i++) {
            if ((Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) > _p) || (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == _p)) {
                all_blocks_list.get(i).put("palette", String.valueOf((long) (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) + 1)));
            }
        }
        FileUtil.writeFile(blocks_dir, new Gson().toJson(all_blocks_list));
        readSettings();
        refresh_list();
    }

    private void _moveRelatedBlocksToRecycleBin(final double _p) {
        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == _p) {
                all_blocks_list.get(i).put("palette", "-1");
            }
        }
        FileUtil.writeFile(blocks_dir, new Gson().toJson(all_blocks_list));
        readSettings();
        refresh_list();
    }

    private void _emptyRecyclebin() {
        List<Map<String, Object>> newBlocks = new LinkedList<>();
        for (int i = 0; i < all_blocks_list.size(); i++) {
            if (!(Double.parseDouble(all_blocks_list.get(i).get("palette").toString()) == -1)) {
                newBlocks.add(all_blocks_list.get(i));
            }
        }
        FileUtil.writeFile(blocks_dir, new Gson().toJson(newBlocks));
        readSettings();
        refresh_list();
    }


    private void showPaletteDialog(boolean isEditing, Integer oldPosition, String oldName, String oldColor, Integer insertAtPosition) {
        aB dialog = new aB(this);
        dialog.a(R.drawable.ic_mtrl_palette);
        dialog.b(!isEditing ? getString(R.string.create_a_new_palette) : getString(R.string.edit_palette));

        DialogPaletteBinding binding = DialogPaletteBinding.inflate(getLayoutInflater());

        if (isEditing) {
            binding.nameEditText.setText(oldName);
            binding.colorEditText.setText(oldColor);
        }

        binding.colorEditText.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.color.setError(null);
            }
        });
        binding.nameEditText.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.name.setError(null);
            }
        });

        binding.openColorPalette.setOnClickListener(v1 -> {
            final Zx zx = new Zx(this, 0, true, false);
            zx.a(new PCP(binding.colorEditText));
            zx.showAtLocation(binding.openColorPalette, Gravity.CENTER, 0, 0);
        });

        dialog.a(binding.getRoot());

        dialog.b(Helper.getResString(R.string.common_word_save), v -> {
            try {
                String nameInput = binding.nameEditText.getText().toString();
                String colorInput = binding.colorEditText.getText().toString();

                if (nameInput.isEmpty()) {
                    binding.name.setError(getString(R.string.name_cannot_be_empty));
                    binding.name.requestFocus();
                    return;
                }

                if (colorInput.isEmpty()) {
                    binding.color.setError(getString(R.string.color_cannot_be_empty));
                    binding.color.requestFocus();
                    return;
                }

                Color.parseColor(colorInput);

                if (!isEditing) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("name", nameInput);
                    map.put("color", colorInput);

                    if (insertAtPosition == null) {
                        pallet_listmap.add(map);
                        FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                        readSettings();
                        refresh_list();
                    } else {
                        pallet_listmap.add(insertAtPosition, map);
                        FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                        readSettings();
                        refresh_list();
                        _insertBlocksAt(insertAtPosition + 9);
                    }
                } else {
                    pallet_listmap.get(oldPosition).put("name", nameInput);
                    pallet_listmap.get(oldPosition).put("color", colorInput);
                    FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                    readSettings();
                    refresh_list();
                }
                dialog.dismiss();
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                binding.color.setError(getString(R.string.malformed_hexadecimal_color));
                binding.color.requestFocus();
            }
        });

        dialog.a(Helper.getResString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));

        dialog.show();
    }


    public class PaletteAdapter extends BaseAdapter {

        private final ArrayList<HashMap<String, Object>> palettes;

        public PaletteAdapter(ArrayList<HashMap<String, Object>> palettes) {
            this.palettes = palettes;
        }

        @Override
        public int getCount() {
            return palettes.size();
        }

        @Override
        public HashMap<String, Object> getItem(int position) {
            return palettes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater _inflater = getLayoutInflater();
            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.pallet_customview, parent, false);
            }

            final TextView sub = convertView.findViewById(R.id.sub);
            final TextView title = convertView.findViewById(R.id.title);
            final LinearLayout color = convertView.findViewById(R.id.color);
            final LinearLayout background = convertView.findViewById(R.id.background);
            final com.google.android.material.card.MaterialCardView background_card = convertView.findViewById(R.id.background_card);

            title.setText(pallet_listmap.get(position).get("name").toString());
            sub.setText(getString(R.string.blocks) + (long) (getN(position + 9)));
            binding.recycleSub.setText(getString(R.string.blocks) + (long) (getN(-1)));

            int backgroundColor;
            String paletteColorValue = (String) palettes.get(position).get("color");
            try {
                backgroundColor = Color.parseColor(paletteColorValue);
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                SketchwareUtil.toastError("Invalid background color '" + paletteColorValue + "' in Palette #" + (position + 1));
                backgroundColor = Color.WHITE;
            }
            color.setBackgroundColor(backgroundColor);

            background_card.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(BlocksManager.this, background);
                Menu menu = popup.getMenu();
                if (position != 0) menu.add(Menu.NONE, 0, Menu.NONE, R.string.move_up);
                if (position != getCount() - 1)
                    menu.add(Menu.NONE, 1, Menu.NONE, R.string.move_down);
                menu.add(Menu.NONE, 2, Menu.NONE, R.string.common_word_edit);
                menu.add(Menu.NONE, 3, Menu.NONE, R.string.common_word_delete);
                menu.add(Menu.NONE, 4, Menu.NONE, R.string.insert);
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 2:
                            showPaletteDialog(true, position,
                                    pallet_listmap.get(position).get("name").toString(),
                                    pallet_listmap.get(position).get("color").toString(), null);
                            break;

                        case 3:
                            new MaterialAlertDialogBuilder(BlocksManager.this)
                                    .setTitle(pallet_listmap.get(position).get("name").toString())
                                    .setMessage(R.string.remove_all_blocks_related_to_this_palette)
                                    .setPositiveButton(R.string.remove_permanently, (dialog, which) -> {
                                        pallet_listmap.remove(position);
                                        FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                                        removeRelatedBlocks(position + 9);
                                        readSettings();
                                        refresh_list();
                                    })
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setNeutralButton(R.string.move_to_recycle_bin, (dialog, which) -> {
                                        _moveRelatedBlocksToRecycleBin(position + 9);
                                        pallet_listmap.remove(position);
                                        FileUtil.writeFile(pallet_dir, new Gson().toJson(pallet_listmap));
                                        removeRelatedBlocks(position + 9);
                                        readSettings();
                                        refresh_list();
                                    }).show();
                            break;

                        case 0:
                            moveUp(position);
                            break;

                        case 1:
                            moveDown(position);
                            break;

                        case 4:
                            showPaletteDialog(false, null, null, null, position);
                            break;

                        default:
                    }
                    return true;
                });
                popup.show();

                return true;
            });

            background_card.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), BlocksManagerDetailsActivity.class);
                intent.putExtra("position", String.valueOf((long) (position + 9)));
                intent.putExtra("dirB", blocks_dir);
                intent.putExtra("dirP", pallet_dir);
                startActivity(intent);
            });

            return convertView;
        }
    }
}
