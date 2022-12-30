package co.pushalert.test.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import co.pushalert.PushAlert;
import co.pushalert.test.databinding.FragmentGeneralBinding;

/**
 * A placeholder fragment containing a simple view.
 */
public class GeneralPlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private FragmentGeneralBinding binding;

    public static GeneralPlaceholderFragment newInstance(int index) {
        GeneralPlaceholderFragment fragment = new GeneralPlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PageViewModel pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentGeneralBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final Button btnAddAttributes = binding.addAttributes;
        btnAddAttributes.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Added Attributes Successfully", Toast.LENGTH_SHORT).show();

            //We recommend to use in-built method to set following parameters:
                // unique id: associateID(String)
                // name: setFirstName(String) and setLastName(String),
                // age: setAge(int),
                // gender: setGender(String),
                // email: setEmail(String),
                // Phone Number: setPhoneNum(String)

            Map<String, String> attr = new HashMap<>();
            attr.put("zip_code", "98125"); //add as many you want
            PushAlert.addAttributes(attr);
        });

        final Button btnAddToSegment = binding.addToSegment;
        btnAddToSegment.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Added to Segment Successfully", Toast.LENGTH_SHORT).show();

            PushAlert.addUserToSegment(20577);
        });

        final Button btnTriggerEvent = binding.triggerEvent;
        btnTriggerEvent.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Event Triggered Successfully", Toast.LENGTH_SHORT).show();

            PushAlert.triggerEvent("videos", "play", "video_id", 132);
        });

        FloatingActionButton fab = binding.fab;

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean alreadySubscribed = PushAlert.requestForPushNotificationPermission(false);
                if(alreadySubscribed){
                    Snackbar.make(view, "User Already Subscribed.", Snackbar.LENGTH_LONG)
                            .setAction("Alert!", null).show();
                }

            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}