package io.github.xausky.unitymodmanager.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.github.xausky.unitymodmanager.R;

public class ModInfoDialog extends AlertDialog {
    public ModInfoDialog(Context context, File[] files, JSONObject info) {
        super(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.mod_info, null, false);
        SliderLayout slider = dialogView.findViewById(R.id.mod_info_slider);
        TextView nameView = dialogView.findViewById(R.id.mod_info_name);
        TextView authorView = dialogView.findViewById(R.id.mod_info_author);
        TextView descriptionView = dialogView.findViewById(R.id.mod_info_description);
        if(files!=null && files.length != 0){
            for(File file:files){
                DefaultSliderView view = new DefaultSliderView(context);
                view.image(file);
                view.setScaleType(BaseSliderView.ScaleType.CenterInside);
                slider.addSlider(view);
            }
        } else {
            slider.setVisibility(View.GONE);
        }
        String name = null;
        String author = null;
        String description = null;
        if(info != null){
            try {
                name = info.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                author = info.getString("author");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                description = info.getString("description");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(name == null){
            nameView.setVisibility(View.GONE);
        } else {
            nameView.setVisibility(View.VISIBLE);
            nameView.setText(String.format(context.getString(R.string.mod_info_name), name));
        }
        if(author == null){
            authorView.setVisibility(View.GONE);
        } else {
            authorView.setVisibility(View.VISIBLE);
            authorView.setText(String.format(context.getString(R.string.mod_info_author), author));
        }
        if(description == null){
            descriptionView.setVisibility(View.GONE);
        } else {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(String.format(context.getString(R.string.mod_info_description), description));
        }
        setView(dialogView);
    }
}
