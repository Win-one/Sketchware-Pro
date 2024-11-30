package mod.jbk.editor.manage.library;

import static pro.sketchware.utility.SketchwareUtil.getDip;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import a.a.a.MA;
import a.a.a.aB;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExcludeBuiltInLibrariesActivity extends BaseAppCompatActivity {
    private static final String TAG = "ExcludeBuiltInLibraries";

    private MaterialSwitch enabled;
    private TextView preview;
    private String sc_id;
    private boolean isExcludingEnabled;
    private List<BuiltInLibraries.BuiltInLibrary> excludedLibraries;
    private Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isStoragePermissionGranted()) {
            finish();
            return;
        }
        setContentView(R.layout.manage_library_exclude_builtin_libraries);

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.exclude_built_in_libraries);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        TextView enable = findViewById(R.id.tv_enable);
        enable.setText(Helper.getResString(R.string.design_library_settings_title_enabled));
        TextView warning = findViewById(R.id.tv_desc);
        warning.setText(R.string.this_might_break_your_project_if_you_don_t_know_what_you_re_doing);
        TextView label = findViewById(R.id.tv_title);
        label.setText(R.string.exclude_built_in_libraries);

        LinearLayout excludedLibraries = findViewById(R.id.item);
        excludedLibraries.setOnClickListener(v -> showSelectBuiltInLibrariesDialog());
        LinearLayout enabledContainer = findViewById(R.id.layout_switch);
        enabledContainer.setOnClickListener(v -> enabled.setChecked(!enabled.isChecked()));
        enabled = findViewById(R.id.lib_switch);
        enabled.setOnCheckedChangeListener((buttonView, isChecked) -> isExcludingEnabled = isChecked);
        preview = findViewById(R.id.item_desc);
        config = readConfig(sc_id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Restore").setIcon(getDrawable(R.drawable.history_24px)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        String title = menuItem.getTitle().toString();
        if (title.equals("Restore")) {
            showResetDialog();
        } else {
            return false;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (config != null && config.first.equals(isExcludingEnabled) && config.second.equals(excludedLibraries)) {
            super.onBackPressed();
        } else {
            k();
            try {
                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() ->
                        new SaveConfigTask(this).execute(), 500);
            } catch (Exception e) {
                onSaveError(e);
            }
        }
    }

    private void onSaveError(Throwable throwable) {
        String errorMessage = "Couldn't save configuration: " + throwable.getMessage();
        LogUtil.e(TAG, errorMessage, throwable);
        onSaveError(errorMessage);
    }

    private void onSaveError(String errorMessage) {
        SketchwareUtil.toastError(errorMessage);
        h();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putBoolean("isExcludingEnabled", isExcludingEnabled);
        outState.putParcelableArrayList("excludedLibraryNames", new ArrayList<>(excludedLibraries));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            isExcludingEnabled = isExcludingEnabled(sc_id);
            excludedLibraries = getExcludedLibraries(sc_id);
        } else {
            isExcludingEnabled = savedInstanceState.getBoolean("isExcludingEnabled");
            excludedLibraries = savedInstanceState.getParcelableArrayList("excludedLibraryNames");
        }

        enabled.setChecked(isExcludingEnabled);
        refreshPreview();
    }

    private void refreshPreview() {
        String libraries = excludedLibraries.stream()
                .map(BuiltInLibraries.BuiltInLibrary::getName)
                .collect(Collectors.joining(", "));
        if (libraries.isEmpty()) {
            libraries = getString(R.string.none_selected_tap_here_to_configure);
        }
        preview.setText(libraries);
    }

    private void showResetDialog() {
        aB dialog = new aB(this);
        dialog.a(R.drawable.rollback_96);
        dialog.b(Helper.getResString(R.string.common_word_reset));
        dialog.a(getString(R.string.reset_excluded_built_in_libraries_this_action_cannot_be_undone));
        dialog.b(Helper.getResString(R.string.common_word_reset), v -> {
            saveConfig(sc_id, false, Collections.emptyList());
            enabled.setChecked(false);
            excludedLibraries = Collections.emptyList();
            refreshPreview();
            dialog.dismiss();
        });
        dialog.a(Helper.getResString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));
        dialog.show();
    }

    private static File getConfigPath(String sc_id) {
        return new File(Environment.getExternalStorageDirectory(),
                ".sketchware" + File.separator + "data" + File.separator + sc_id + File.separator + "excluded_library");
    }

    private static void saveConfig(String sc_id, boolean isExcludingEnabled, List<BuiltInLibraries.BuiltInLibrary> excludedLibraries) {
        List<String> excludedLibraryNames = excludedLibraries.stream()
                .map(BuiltInLibraries.BuiltInLibrary::getName)
                .collect(Collectors.toList());
        Pair<Boolean, List<String>> config = new Pair<>(isExcludingEnabled, excludedLibraryNames);
        FileUtil.writeFile(getConfigPath(sc_id).getAbsolutePath(), new Gson().toJson(config));
    }

    @Nullable
    private static Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> readConfig(String sc_id) {
        File configPath = getConfigPath(sc_id);
        if (configPath.isFile()) {
            String content = FileUtil.readFile(configPath.getAbsolutePath());

            String errorMessage;
            try {
                Pair<Boolean, List<String>> config = new Gson().fromJson(content, new TypeToken<>() {
                });
                if (config != null) {
                    List<BuiltInLibraries.BuiltInLibrary> libraries = config.second.stream()
                            .map(s -> {
                                Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(s);
                                return library.orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return new Pair<>(config.first, libraries);
                }
                errorMessage = "read config was null";
                // fall-through to shared handler
            } catch (Exception e) {
                errorMessage = Log.getStackTraceString(e);
                // fall-through to shared handler
            }

            LogUtil.e(TAG, "Couldn't parse config: " + errorMessage);
        }
        return null;
    }

    public static boolean isExcludingEnabled(String sc_id) {
        Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config = readConfig(sc_id);
        if (config != null) {
            return config.first;
        } else {
            return false;
        }
    }

    @NonNull
    public static List<BuiltInLibraries.BuiltInLibrary> getExcludedLibraries(String sc_id) {
        Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config = readConfig(sc_id);
        if (config != null) {
            return config.second;
        } else {
            return Collections.emptyList();
        }
    }

    @DrawableRes
    public static int getItemIcon() {
        return R.drawable.ic_detail_setting_48dp;
    }

    public static String getItemTitle() {
        return Helper.getResString(R.string.exclude_built_in_libraries);
    }

    public static String getDefaultItemDescription() {
        return Helper.getResString(R.string.use_custom_library_versions);
    }

    public static String getSelectedLibrariesItemDescription() {
        return Helper.getResString(R.string.built_in_libraries_excluded);
    }

    private void showSelectBuiltInLibrariesDialog() {
        aB dialog = new aB(this);
        dialog.b(getString(R.string.select_built_in_libraries));
        RecyclerView list = new RecyclerView(this);
        list.setPadding(
            (int) getDip(20),
            (int) getDip(8),
            (int) getDip(20),
            (int) getDip(0)
        );

        // magic to initialize scrollbars even without android:scrollbars defined in XML
        // https://stackoverflow.com/a/48698300/10929762
        TypedArray typedArray = obtainStyledAttributes(null, new int[0]);
        try {
            //noinspection JavaReflectionMemberAccess
            Method method = View.class.getDeclaredMethod("initializeScrollbars", TypedArray.class);
            method.setAccessible(true);
            method.invoke(list, typedArray);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LogUtil.e(TAG, "Couldn't add scrollbars to RecyclerView", e);
        }
        typedArray.recycle();
        list.setVerticalScrollBarEnabled(true);

        list.setLayoutManager(new LinearLayoutManager(null));
        BuiltInLibraryAdapter adapter = new BuiltInLibraryAdapter(excludedLibraries);
        adapter.setHasStableIds(true);
        list.setAdapter(adapter);
        dialog.a(list);
        dialog.a(Helper.getResString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));
        dialog.b(Helper.getResString(R.string.common_word_save), v -> {
            excludedLibraries = adapter.getSelectedBuiltInLibraries();
            dialog.dismiss();
            refreshPreview();
        });
        dialog.show();
    }

    private static class SaveConfigTask extends MA {
        private final WeakReference<ExcludeBuiltInLibrariesActivity> activity;

        public SaveConfigTask(ExcludeBuiltInLibrariesActivity activity) {
            super(activity);
            this.activity = new WeakReference<>(activity);
            activity.a(this);
        }

        @Override
        public void a() {
            activity.get().h();
            activity.get().setResult(RESULT_OK);
            activity.get().finish();
        }

        @Override
        public void a(String s) {
            activity.get().onSaveError("Couldn't save configuration: " + s);
        }

        @Override
        public void b() {
            saveConfig(activity.get().sc_id, activity.get().isExcludingEnabled, activity.get().excludedLibraries);
        }

    }

    private static class BuiltInLibraryAdapter extends RecyclerView.Adapter<BuiltInLibraryAdapter.ViewHolder> {
        private final List<BuiltInLibraries.BuiltInLibrary> libraries;
        private final Map<Integer, Void> checkedIndices;

        public BuiltInLibraryAdapter(List<BuiltInLibraries.BuiltInLibrary> excludedLibraries) {
            libraries = Arrays.asList(BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES);
            libraries.sort(Comparator.comparing(BuiltInLibraries.BuiltInLibrary::getName, String.CASE_INSENSITIVE_ORDER));
            checkedIndices = new HashMap<>();

            for (BuiltInLibraries.BuiltInLibrary excludedLibrary : excludedLibraries) {
                int index = libraries.indexOf(excludedLibrary);
                if (index >= 0) {
                    checkedIndices.put(index, null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return libraries.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_library_exclude_builtin_libraries_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BuiltInLibraries.BuiltInLibrary library = libraries.get(position);
            holder.selected.setChecked(checkedIndices.containsKey(position));
            holder.name.setText(library.getName());
            Optional<String> packageName = library.getPackageName();
            if (packageName.isPresent()) {
                holder.packageName.setVisibility(View.VISIBLE);
                holder.packageName.setText(packageName.get());
            } else {
                holder.packageName.setVisibility(View.GONE);
            }

            View.OnClickListener selectingListener = v -> {
                CheckBox selected = holder.selected;
                if (v.getId() != R.id.chk_select) {
                    selected.setChecked(!selected.isChecked());
                }

                if (selected.isChecked()) {
                    checkedIndices.put(position, null);
                } else {
                    checkedIndices.remove(position);
                }
            };
            holder.selected.setOnClickListener(selectingListener);
            holder.selectableItem.setOnClickListener(selectingListener);
        }

        public List<BuiltInLibraries.BuiltInLibrary> getSelectedBuiltInLibraries() {
            Set<Integer> checkedIndicesKeySet = checkedIndices.keySet();
            List<BuiltInLibraries.BuiltInLibrary> selectedLibraries = new ArrayList<>(checkedIndicesKeySet.size());
            for (int i : checkedIndicesKeySet) {
                selectedLibraries.add(libraries.get(i));
            }
            return selectedLibraries;
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            public final LinearLayout selectableItem;
            public final CheckBox selected;
            public final TextView name;
            public final TextView packageName;

            public ViewHolder(View itemView) {
                super(itemView);
                selectableItem = itemView.findViewById(R.id.view_item);
                selected = itemView.findViewById(R.id.chk_select);
                name = itemView.findViewById(R.id.tv_screen_name);
                packageName = itemView.findViewById(R.id.tv_activity_name);
            }
        }
    }
}