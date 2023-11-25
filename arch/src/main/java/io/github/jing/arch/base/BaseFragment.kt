package io.github.jing.arch.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 基类
 */
abstract class BaseFragment<VB: ViewBinding>: Fragment() {

    lateinit var vb: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = onCreateViewBinding(inflater,  container)
        return vb.root
    }

    abstract fun onCreateViewBinding(inflater: LayoutInflater,
                                      container: ViewGroup?): VB
}