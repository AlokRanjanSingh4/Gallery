package com.alok.gallery.ui_handling;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.alok.gallery.R;
import com.alok.gallery.ui_handling.fragment.GridFragment;

public class MainActivity extends AppCompatActivity {

  public static int currentPosition;
  private static final String KEY_CURRENT_POSITION = "com,alok.gallery.ui.key.currentPosition";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (savedInstanceState != null) {
      currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
      // Return here to prevent adding additional GridFragments when changing orientation.
      return;
    }
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager
        .beginTransaction()
        .add(R.id.fragment_container, new GridFragment(), GridFragment.class.getSimpleName())
        .commit();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_CURRENT_POSITION, currentPosition);
  }
}
