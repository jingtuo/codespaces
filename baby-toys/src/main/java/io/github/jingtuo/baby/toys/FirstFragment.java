package io.github.jingtuo.baby.toys;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import io.github.jingtuo.baby.toys.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    private ReplayToy replayToy;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
        replayToy = new ReplayToy(getContext());
        replayToy.setOnStatusChangedListener(new ReplayToy.OnStatusChangedListener() {
            @Override
            public void onStatusChanged(int recorderStatus, int playerStatus) {
                if (replayToy.isRecording()) {
                    //
                    binding.textviewFirst.setText(R.string.recording);
                }
                if (replayToy.isPlaying()) {
                    binding.textviewFirst.setText(R.string.playing);
                }
                if (replayToy.isStopped()) {
                    binding.textviewFirst.setText(R.string.hello_first_fragment);
                }
            }
        });
        binding.btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (replayToy.isRecording() || replayToy.isPlaying()) {
                    replayToy.stop();
                    binding.btnReplay.setText(R.string.start);
                } else {
                    replayToy.start();
                    binding.btnReplay.setText(R.string.stop);
                }
            }
        });
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {

        }).launch(Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}