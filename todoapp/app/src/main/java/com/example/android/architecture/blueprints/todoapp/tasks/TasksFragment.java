/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.tasks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;

import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.ScrollChildSwipeRefreshLayout;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.databinding.TaskItemBinding;
import com.example.android.architecture.blueprints.todoapp.databinding.TasksFragBinding;
import com.example.android.architecture.blueprints.todoapp.util.SnackbarUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


/**
 * Display a grid of {@link Task}s. User can choose to view all, active or completed tasks.
 */
public class TasksFragment extends Fragment {

    private TasksViewModel mTasksViewModel;

    private TasksFragBinding mTasksFragBinding;

    private TasksAdapter mListAdapter;

    private Observable.OnPropertyChangedCallback mSnackbarCallback;


    public TasksFragment() {
        // Requires empty public constructor
    }

    public static TasksFragment newInstance() {
        return new TasksFragment();
    }




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTasksFragBinding = TasksFragBinding.inflate(inflater, container, false);
        mTasksFragBinding.setView(this);
        mTasksFragBinding.setViewmodel(mTasksViewModel);

        setHasOptionsMenu(true);

        return mTasksFragBinding.getRoot();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setup();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTasksViewModel.start();
    }

    @Override
    public void onDestroy() {
        mListAdapter.onDestroy();
        if (mSnackbarCallback != null) {
            mTasksViewModel.snackbarText.removeOnPropertyChangedCallback(mSnackbarCallback);
        }
        super.onDestroy();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tasks_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                mTasksViewModel.clearCompletedTasks();
                break;
            case R.id.menu_filter:
                showFilteringPopUpMenu();
                break;
            case R.id.menu_refresh:
                mTasksViewModel.loadTasks(true);
                break;
        }
        return true;
    }




    public void setViewModel(TasksViewModel viewModel) {
        mTasksViewModel = viewModel;
    }


    private void showFilteringPopUpMenu() {
        PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter));
        popup.getMenuInflater().inflate(R.menu.filter_tasks, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.active:
                    mTasksViewModel.setFiltering(TasksFilterType.ACTIVE_TASKS);
                    break;
                case R.id.completed:
                    mTasksViewModel.setFiltering(TasksFilterType.COMPLETED_TASKS);
                    break;
                default:
                    mTasksViewModel.setFiltering(TasksFilterType.ALL_TASKS);
                    break;
            }
            mTasksViewModel.loadTasks(false);
            return true;
        });

        popup.show();
    }




    private void setup() {
        setupSnackbar();
        setupFab();
        setupListAdapter();
        setupRefreshLayout();
    }

    private void setupSnackbar() {
        mSnackbarCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                SnackbarUtils.showSnackbar(getView(), mTasksViewModel.getSnackbarText());
            }
        };
        mTasksViewModel.snackbarText.addOnPropertyChangedCallback(mSnackbarCallback);
    }

    private void setupFab() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab_add_task);
        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> mTasksViewModel.addNewTask());
    }

    private void setupListAdapter() {
        ListView listView =  mTasksFragBinding.tasksList;

        mListAdapter = new TasksAdapter(
                new ArrayList<>(0),
                (TasksActivity) getActivity(),
                Injection.provideTasksRepository(getContext().getApplicationContext()),
                mTasksViewModel);
        listView.setAdapter(mListAdapter);
    }

    private void setupRefreshLayout() {
        ListView listView =  mTasksFragBinding.tasksList;

        ScrollChildSwipeRefreshLayout swipeRefreshLayout = mTasksFragBinding.refreshLayout;
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
        );
        // Set the scrolling view in the custom SwipeRefreshLayout.
        swipeRefreshLayout.setScrollUpChild(listView);
    }




    public static class TasksAdapter extends BaseAdapter {

        @Nullable private TaskItemNavigator mTaskItemNavigator;

        private final TasksViewModel mTasksViewModel;

        private List<Task> mTasks;

        private TasksRepository mTasksRepository;

        public TasksAdapter(List<Task> tasks, TasksActivity taskItemNavigator,
                            TasksRepository tasksRepository,
                            TasksViewModel tasksViewModel) {
            mTaskItemNavigator = taskItemNavigator;
            mTasksRepository = tasksRepository;
            mTasksViewModel = tasksViewModel;
            setList(tasks);
        }

        public void onDestroy() {
            mTaskItemNavigator = null;
        }

        public void replaceData(List<Task> tasks) {
            setList(tasks);
        }

        @Override
        public int getCount() {
            return mTasks != null ? mTasks.size() : 0;
        }

        @Override
        public Task getItem(int i) {
            return mTasks.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Task task = getItem(i);
            TaskItemBinding binding;
            if (view == null) {
                // Inflate
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

                // Create the binding
                binding = TaskItemBinding.inflate(inflater, viewGroup, false);
            } else {
                // Recycling view
                binding = DataBindingUtil.getBinding(view);
            }

            final TaskItemViewModel viewmodel = new TaskItemViewModel(
                    viewGroup.getContext().getApplicationContext(),
                    mTasksRepository
            );

            viewmodel.setNavigator(mTaskItemNavigator);

            binding.setViewmodel(viewmodel);
            // To save on PropertyChangedCallbacks, wire the item's snackbar text observable to the
            // fragment's.
            viewmodel.snackbarText.addOnPropertyChangedCallback(
                    new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable observable, int i) {
                    mTasksViewModel.snackbarText.set(viewmodel.getSnackbarText());
                }
            });
            viewmodel.setTask(task);

            return binding.getRoot();
        }


        private void setList(List<Task> tasks) {
            mTasks = tasks;
            notifyDataSetChanged();
        }
    }
}
