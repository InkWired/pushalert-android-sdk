package co.pushalert.test.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;

import co.pushalert.PushAlert;
import co.pushalert.test.databinding.FragmentEcommerceBinding;

/**
 * A placeholder fragment containing a simple view.
 */
public class ECommercePlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private FragmentEcommerceBinding binding;
    private double total_amount =0;
    private int total_items = 0;

    public static ECommercePlaceholderFragment newInstance(int index) {
        ECommercePlaceholderFragment fragment = new ECommercePlaceholderFragment();
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

        binding = FragmentEcommerceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView txtTotalAmount = binding.totalAmount;

        final Button btnItem1 = binding.item1Btn;
        btnItem1.setOnClickListener(v -> {
            if(btnItem1.getText().equals("Add to Cart")) {
                total_amount += 4.99;
                total_items++;
                btnItem1.setText("Remove");
            }
            else{
                total_amount -= 4.99;
                total_items--;
                btnItem1.setText("Add to Cart");
            }
            setTotalAmount(txtTotalAmount, total_amount);
        });

        final Button btnItem2 = binding.item2Btn;
        btnItem2.setOnClickListener(v -> {
            if(btnItem2.getText().equals("Add to Cart")) {
                total_amount += 19.99;
                total_items++;
                btnItem2.setText("Remove");
            }
            else{
                total_amount -= 19.99;
                total_items--;
                btnItem2.setText("Add to Cart");
            }
            setTotalAmount(txtTotalAmount, total_amount);
        });

        final Button btnItem3 = binding.item3Btn;
        btnItem3.setOnClickListener(v -> {
            if(btnItem3.getText().equals("Add to Cart")) {
                total_amount += 2.99;
                total_items++;
                btnItem3.setText("Remove");
            }
            else{
                total_amount -= 2.99;
                total_items--;
                btnItem3.setText("Add to Cart");
            }
            setTotalAmount(txtTotalAmount, total_amount);
        });


        final Button btnCompleteOrder = binding.completeOrder;
        btnCompleteOrder.setOnClickListener(v -> {
            //Report the conversion
            PushAlert.reportConversionWithValue("Purchase", total_amount);

            total_items = 0;
            total_amount = 0;
            btnItem1.setText("Add to Cart");
            btnItem2.setText("Add to Cart");
            btnItem3.setText("Add to Cart");

            setTotalAmount(txtTotalAmount, total_amount);

            Toast.makeText(getContext(), "Order successfully placed!", Toast.LENGTH_LONG).show();
        });


        final Button btnOutOfStockAlerts = binding.btnOutOfStockAlerts;
        btnOutOfStockAlerts.setOnClickListener(v -> {
            binding.outOfStockUI.setVisibility(View.VISIBLE);
        });

        final Button btnPriceDropAlerts = binding.btnPriceDropAlerts;
        btnPriceDropAlerts.setOnClickListener(v -> {
            binding.priceDropUI.setVisibility(View.VISIBLE);
        });

        final TextView closeOutOfStockUI = binding.closeOutOfStockUI;
        closeOutOfStockUI.setOnClickListener(v -> {
            binding.outOfStockUI.setVisibility(View.GONE);
        });

        final TextView closePriceDropUI = binding.closePriceDropUI;
        closePriceDropUI.setOnClickListener(v -> {
            binding.priceDropUI.setVisibility(View.GONE);
        });


        final Button btnOutOfStockAlertsUI = binding.btnOutOfStockAlertsUI;
        if(PushAlert.isOutOfStockEnabled(2432, 1)){
            //Here you can disable button or change to remove alert
        }
        btnOutOfStockAlertsUI.setOnClickListener(v -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", "Alex");

            PushAlert.addOutOfStockAlert(2432, 1, 19.99, map);

            Toast.makeText(getContext(), "Out of Stock Alert Successfully Registered!", Toast.LENGTH_SHORT).show();
        });

        final Button btnPriceDropAlertsUI = binding.btnPriceDropAlertsUI;
        if(PushAlert.isPriceDropEnabled(2431, 1)){
            //Here you can disable button or change to remove alert
        }
        btnPriceDropAlertsUI.setOnClickListener(v -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", "Mohit");

            PushAlert.addPriceDropAlert(2431, 1, 4.99, map);

            Toast.makeText(getContext(), "Price Drop Alert Successfully Registered!", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    public void setTotalAmount(TextView tv, double total_amount){
        double roundOff = (double) Math.round(total_amount * 100) / 100;
        if(roundOff==0 || total_items==0){
            PushAlert.processAbandonedCart(PushAlert.AbandonedCartAction.DELETE, null);
        }
        else{
            Map<String, String> data = new HashMap<>();
            data.put("customer_name", "Alex");
            data.put("image", "https://cdn.pushalert.co/large-image/image9-16_test.png");
            data.put("total_items", String.valueOf(total_items));
            data.put("total_amount", String.valueOf(roundOff));
            data.put("cart_url", "https://korner.space/blog/cart/");
            data.put("checkout_url", "https://korner.space/blog/checkout/");

            PushAlert.processAbandonedCart(PushAlert.AbandonedCartAction.UPDATE, data);
        }
        tv.setText("Total Amount: $" + roundOff);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}