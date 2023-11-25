package io.github.jing.arch.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * 基类
 */
abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {

    lateinit var vb: VB

    //actionBar的返回按钮可用
    private var backEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //使用App可以绘制到系统栏(导航栏、状态栏)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        vb = onCreateViewBinding()
        setContentView(vb.root)
        initView()
        if (!initData()){
            finish()
            return
        }
    }

    /**
     * 初始化数据, 数据检查成功返回true, 数据检查失败返回false, 销毁当前页面
     */
    open fun initData(): Boolean = true

    abstract fun onCreateViewBinding(): VB

    fun enableBack() {
        backEnabled = true
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (backEnabled && item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    open fun initView() {

    }
}