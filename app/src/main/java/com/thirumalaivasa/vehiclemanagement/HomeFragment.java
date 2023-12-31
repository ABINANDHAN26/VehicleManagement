package com.thirumalaivasa.vehiclemanagement;

import static com.thirumalaivasa.vehiclemanagement.Utils.Util.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thirumalaivasa.vehiclemanagement.Adapters.ExpenseListAdapter;
import com.thirumalaivasa.vehiclemanagement.Helpers.RoomDbHelper;
import com.thirumalaivasa.vehiclemanagement.Models.ExpenseData;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager rvl;
    private ExpenseListAdapter rva;
    private AutoCompleteTextView vehicleSpinner, serviceSpinner;

    private String selectedVehicle, selectedService;

    private List<ExpenseData> expenseDataArrayList;
    private List<ExpenseData> initialData;
    private List<String> vehicleNumList;

    private List<String> driverNameList;

    private boolean isLoading;
    private boolean isLastPage;
    private int currentPage;

    private RoomDbHelper dbHelper;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        findViews(view);

        return view;
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int pos = viewHolder.getAdapterPosition();
            initialData.remove(pos);
            rva.notifyItemRemoved(pos);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        dbHelper = RoomDbHelper.getInstance(getContext());
        expenseDataArrayList = dbHelper.expenseDao().getAllExpenses();

        for (int i = 0; i < expenseDataArrayList.size(); i++) {
            expenseDataArrayList.get(i).setOdometer(i + 1);
        }

        Log.i(TAG, "onResume: " + expenseDataArrayList.size());
        vehicleNumList = dbHelper.vehicleDao().getAllVehicleNumber();
        driverNameList = dbHelper.driverDao().getDriversName();

        setInitialData();
        rva.setHasStableIds(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(rvl);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(100);
        recyclerView.setAdapter(rva);
        ItemTouchHelper helper = new ItemTouchHelper(simpleCallback);
        helper.attachToRecyclerView(recyclerView);
        loadPage();
        if (expenseDataArrayList != null && expenseDataArrayList.size() > 0) {
            setFilterSpinner(0);
        }

        setServiceSpinner();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    loadPage();
                }
            }
        });

        vehicleSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedVehicle = vehicleSpinner.getText().toString();
            List<ExpenseData> filteredList = filter(selectedVehicle, serviceSpinner.getText().toString());
            rva = new ExpenseListAdapter(getActivity(), filteredList);
            recyclerView.setAdapter(rva);
        });


        serviceSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedService = serviceSpinner.getText().toString();
            if (selectedService.equals("Salary")) {
                setFilterSpinner(1);
            } else {
                setFilterSpinner(0);
            }
            List<ExpenseData> filteredList = filter(selectedVehicle, serviceSpinner.getText().toString());
            rva = new ExpenseListAdapter(getActivity(), filteredList);
            recyclerView.setAdapter(rva);
        });


    }

    private void setInitialData() {
        initialData = new ArrayList<>();
        setFilterSpinner(0);
        selectedVehicle = "All";
        selectedService = "All";
        isLoading = false;
        isLastPage = false;
        currentPage = 0;
        rvl = new LinearLayoutManager(getActivity());
        rva = new ExpenseListAdapter(getActivity(), initialData);
    }


    private void loadPage() {
        if (isLoading || isLastPage) {
            return;
        }
        //progressBar.setVisibility(View.VISIBLE);
        isLoading = true;
        ArrayList<ExpenseData> newObjects = loadDataForPage(currentPage + 1);

        if (newObjects.size() == 0) {
            isLastPage = true;
        } else {
            currentPage++;
            initialData.addAll(newObjects);
            rva.notifyDataSetChanged();
        }
        // progressBar.setVisibility(View.GONE);
        isLoading = false;
    }

    private ArrayList<ExpenseData> loadDataForPage(int page) {
        ArrayList<ExpenseData> newData = new ArrayList<>();
        int startIndex = (page == 1) ? 0 : ((page - 1) * 10);
        int endIndex = Math.min(startIndex + 10, expenseDataArrayList.size());
        for (int i = startIndex; i < endIndex; i++) {
            newData.add(expenseDataArrayList.get(i));
        }
        return newData;
    }


    private List<ExpenseData> filter(String selectedVehicle, String selectedService) {
        List<ExpenseData> retValue = new ArrayList<>();
        if (selectedVehicle.equals("All") && selectedService.equals("All"))
            return expenseDataArrayList;
        else if (selectedVehicle.equals("All")) {
            for (ExpenseData expenseData : expenseDataArrayList) {
                if (expenseData.getExpenseType() == null)
                    continue;
                if (expenseData.getExpenseType().equals(selectedService))
                    retValue.add(expenseData);
            }
        } else if (selectedService.equals("All")) {
            for (ExpenseData expenseData : expenseDataArrayList) {
                if (expenseData.getVno() == null)
                    continue;
                if (expenseData.getVno().equals(selectedVehicle))
                    retValue.add(expenseData);
            }
        } else if (selectedService.equals("Salary")) {
            for (ExpenseData expenseData : expenseDataArrayList) {
                if (expenseData.getExpenseType() == null || expenseData.getDriverName() == null)
                    continue;
                if (expenseData.getDriverName().equals(selectedVehicle))
                    retValue.add(expenseData);
            }
        } else {
            for (ExpenseData expenseData : expenseDataArrayList) {
                if (expenseData.getVno() == null || expenseData.getExpenseType() == null)
                    continue;

                if (expenseData.getVno().equals(selectedVehicle))
                    if (expenseData.getExpenseType().equals(selectedService))
                        retValue.add(expenseData);
            }
        }

        return retValue;
    }

    //Sets the spinner values for vehicleSpinner
    // The spinner values will be changed  when the service spinner value is salary
    //When salary selected driver names will be listed
    //Otherwise Vehicle numbers will be listed
    private void setFilterSpinner(int what) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.drop_down_item);
        arrayAdapter.add("All");
        selectedVehicle = "All";
        switch (what) {
            case 0:
                if (vehicleNumList != null && vehicleNumList.size() > 0) {
                    arrayAdapter.addAll(vehicleNumList);
                    arrayAdapter.notifyDataSetChanged();
                    vehicleSpinner.setText(arrayAdapter.getItem(0));
                    vehicleSpinner.setAdapter(arrayAdapter);
                }
                break;
            case 1:
                if (driverNameList != null && driverNameList.size() > 0) {
                    arrayAdapter.addAll(driverNameList);
                    arrayAdapter.notifyDataSetChanged();
                    vehicleSpinner.setText(arrayAdapter.getItem(0));
                    vehicleSpinner.setAdapter(arrayAdapter);
                }
                break;
        }

    }

    private void setServiceSpinner() {

        String[] expenseArray = getResources().getStringArray(R.array.expenses_types);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.drop_down_item, expenseArray);
        arrayAdapter.notifyDataSetChanged();
        serviceSpinner.setText(arrayAdapter.getItem(0));
        serviceSpinner.setAdapter(arrayAdapter);
    }


    private void findViews(View view) {
        recyclerView = view.findViewById(R.id.home_list_rv);
        vehicleSpinner = view.findViewById(R.id.vehicle_spinner);
        serviceSpinner = view.findViewById(R.id.service_spinner);

    }
}