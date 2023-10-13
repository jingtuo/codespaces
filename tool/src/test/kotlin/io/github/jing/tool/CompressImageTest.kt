package io.github.jing.tool

import kotlin.test.Test


class CompressImageTest {

    @Test
    fun testCompress() {
        val data = listOf(
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-xxhdpi\\care_elders_introduce_light.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxxhdpi\\dialog_ipo_lottery_remind_bottom_layout.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-xxhdpi\\care_elders_introduce_dark.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-xxhdpi\\security_center_bg.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\ipo_notice_img.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\ipo_notice_img_trans.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade\\src\\main\\res\\drawable-xhdpi\\ipo_remind_middle.9.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_sharetransfer\\src\\main\\res\\drawable-xxxhdpi\\hs_pictures_xinzhai.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\hs_pictures_xinzhai.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_sharetransfer\\src\\main\\res\\drawable-xxxhdpi\\hs_pictures_xingu.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\hs_pictures_xingu.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-xhdpi\\care_elders_introduce_light.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\bg_ipo_lottery.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-xhdpi\\care_elders_introduce_dark.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quotes\\hs_quote_widget\\src\\main\\res\\drawable-hdpi\\bgd.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxxhdpi\\icon_list_empty.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\mini_module_preview_hot_form.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade_general\\src\\main\\res\\drawable-xxxhdpi\\icon_ipo_no_data.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\skin_std_my_stock_empty_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_securities\\hs_GHZQ\\src\\main\\res\\drawable-xxxhdpi\\mini_module_preview_market_monitor.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxxhdpi\\skin_mygroup_add_day.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxhdpi\\icon_list_empty.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-mdpi\\care_elders_introduce_light.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xhdpi\\mini_module_preview_quote_index.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_person\\src\\main\\res\\drawable-mdpi\\care_elders_introduce_dark.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\skin_icon_monitory_empty_day.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xhdpi\\common_splash_ad_default.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_securities\\hs_GHZQ\\src\\main\\res\\drawable-xxhdpi\\ghzq_search_result_no_data_light.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxhdpi\\skin_icon_issue_empty_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\skin_std_my_stock_empty_day.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\mini_module_preview_change_sprite.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_home\\src\\main\\res\\drawable-xxxhdpi\\ghzq_icon_counselor.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\mini_module_preview_market_monitor.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_securities\\hs_GHZQ\\src\\main\\res\\drawable-xxxhdpi\\bg_new_stock.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xhdpi\\skin_std_my_stock_empty_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\functional_components\\hs_widget\\src\\main\\res\\drawable-xxxhdpi\\skin_icon_issue_empty_day.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxhdpi\\common_logo_system_update.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxhdpi\\common_banner_home_integration_default.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade\\src\\main\\res\\drawable-xxhdpi\\st_adequacy.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\mini_module_preview_stock_rank.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xhdpi\\mini_module_preview_change_sprite.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_common\\src\\main\\res\\drawable-xxxhdpi\\dialog_ipo_lottery_remind_bottom_button.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_trades\\hs_trade\\src\\main\\res\\drawable-xxhdpi\\mypurchase_bg.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\yuan_shengang_explain.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\mini_module_preview_quote_index.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\skin_icon_monitory_empty_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\functional_components\\hs_widget\\src\\main\\res\\drawable-xxxhdpi\\gm_succeed_icon.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\yuan_hugang_explain.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\functional_components\\hs_widget\\src\\main\\res\\drawable-xxxhdpi\\gm_failed_icon.png",
            "D:\\Projects\\develop-20230719\\AutoPacking\\GHZQ\\Picture\\3x\\about.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\mini_module_preview_hot_block.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxhdpi\\my_stock_mutil_guid.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\shenzheng_explain.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_securities\\hs_GHZQ\\src\\main\\res\\drawable-xhdpi\\ghzq_search_result_no_data_light.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\functional_components\\skin_module\\src\\main\\res\\drawable-xxxhdpi\\skin_no_data_image_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\functional_components\\skin_module\\src\\main\\res\\drawable-xxxhdpi\\skin_no_data_image_day.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_securities\\hs_GHZQ\\src\\main\\res\\drawable-xxhdpi\\ghzq_search_result_no_data_dark.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_home\\src\\main\\res\\drawable-xxhdpi\\banner_home_top_default.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\skin_mygroup_add_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xxxhdpi\\skin_std_my_stock_empty_night.png",
            "D:\\Projects\\develop-20230719\\TZYJ_Android\\hs_quote\\src\\main\\res\\drawable-xhdpi\\mini_module_preview_stock_rank.png"
        )
        val compress = CompressImage()
        compress.compressByTinify(data)
    }

}