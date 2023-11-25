package io.github.jingtuo.laboratory

import androidx.core.view.get
import androidx.core.view.size
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainPageChangeCallback(private val bottomNav: BottomNavigationView): OnPageChangeCallback() {

    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        val count = bottomNav.menu.size
        if (position in 0..<count) {
            val itemId = bottomNav.menu[position]
            if (itemId.itemId != bottomNav.selectedItemId) {
                bottomNav.selectedItemId = itemId.itemId
            }
        }

    }

}