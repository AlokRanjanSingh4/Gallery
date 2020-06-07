package com.alok.gallery.ui_handling.adapter;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.alok.gallery.ui_handling.api_handling.pojo.Value;
import com.alok.gallery.ui_handling.fragment.ImageFragment;

import java.util.List;

public class ImagePagerAdapter extends FragmentStatePagerAdapter {
  private List<Value> arrayImages;
  private int viewFrom;

  public ImagePagerAdapter(Fragment fragment, List<Value> arrayImages, int viewFrom) {
    super(fragment.getChildFragmentManager());
    this.arrayImages=arrayImages;
    this.viewFrom=viewFrom;
  }

  @Override
  public int getCount() {
    return arrayImages.size();
  }

  @Override
  public Fragment getItem(int position) {
    return ImageFragment.newInstance(arrayImages.get(position),viewFrom);
  }
}
