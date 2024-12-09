package pro.sketchware.control;

import android.text.TextUtils;
import android.view.LayoutInflater;

import com.besome.sketch.projects.MyProjectSettingActivity;

import a.a.a.aB;
import a.a.a.mB;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAdvancedVersionControlBinding;
import pro.sketchware.lib.validator.VersionNamePostfixValidator;

public class VersionDialog {
    private final MyProjectSettingActivity activity;
    private final DialogAdvancedVersionControlBinding binding;

    public VersionDialog(MyProjectSettingActivity activity) {
        this.activity = activity;
        LayoutInflater inflater = LayoutInflater.from(activity);
        binding = DialogAdvancedVersionControlBinding.inflate(inflater);
    }

    public void show() {
        final aB dialog = new aB(activity);
        dialog.a(R.drawable.numbers_48);
        dialog.b(Helper.getResString(R.string.advanced_version_control));

        binding.versionCode.setText(String.valueOf(Integer.parseInt(activity.binding.verCode.getText().toString())));
        binding.versionName1.setText(activity.binding.verName.getText().toString().split(" ")[0]);
        if (activity.binding.verName.getText().toString().split(" ").length > 1)
            binding.versionName2.setText(activity.binding.verName.getText().toString().split(" ")[1]);

        dialog.a(binding.getRoot());
        dialog.b(activity.getString(R.string.common_word_save), v -> {
            final String verCode = binding.versionCode.getText().toString();
            final String verName = binding.versionName1.getText().toString();
            final String verNamePostfix = binding.versionName2.getText().toString();

            boolean validVerCode = !TextUtils.isEmpty(verCode);
            boolean validVerName = !TextUtils.isEmpty(verName);

            if (validVerCode) {
                binding.versionCode.setError(null);
            } else {
                binding.versionCode.setError(Helper.getResString(R.string.invalid_version_code));
            }

            if (validVerName) {
                binding.versionName1.setError(null);
            } else {
                binding.versionName1.setError(Helper.getResString(R.string.invalid_version_name));
            }

            if (!mB.a() && validVerCode && validVerName) {
                activity.binding.verCode.setText(verCode);
                activity.binding.verName.setText(!verNamePostfix.isEmpty() ? (verName + " " + verNamePostfix) : verName);
                dialog.dismiss();
            }
        });

        binding.versionName2.addTextChangedListener(new VersionNamePostfixValidator(activity, binding.tilVersionNameExtra));
        dialog.a(activity.getString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));
        dialog.show();
    }
}
