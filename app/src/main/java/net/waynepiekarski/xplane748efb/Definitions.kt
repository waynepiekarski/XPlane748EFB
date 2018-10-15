// ---------------------------------------------------------------------
//
// XPlane748EFB
//
// Copyright (C) 2018 Wayne Piekarski
// wayne@tinmith.net http://tinmith.net/wayne
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// ---------------------------------------------------------------------

package net.waynepiekarski.xplane748efb

import android.util.Log
import kotlin.math.absoluteValue

object Definitions {

    // Constants for each aircraft configuration
    enum class Aircraft {
        SSG
    }

    fun setAircraft(type: Aircraft) {
        if (type == Aircraft.SSG) {
            Log.d(Const.TAG, "Changing to SSG aircraft definition")
            buttons = EFBButtonsSSG748
        }
    }

    // Note that I resized the image by 2x to improve the draw resolution, so all these coordinates
    // were computed from the previous image and scaled up.
    class Button(val _x1: Int, val _y1: Int, val _x2: Int, val _y2: Int, var drawLabel: String? = null) {
        var x1 = _x1 * 2
        var y1 = _y1 * 2
        var x2 = _x2 * 2
        var y2 = _y2 * 2
    }

    val EFBButtonsSSG748 = mapOf(
            "FB LK 1" to Button( 12, 112,  35, 127, drawLabel="--"),
            "FB RK 1" to Button(397, 110, 420, 125, drawLabel="--"),
            "FB LK 2" to Button( 12, 168,  35, 184, drawLabel="--"),
            "FB RK 2" to Button(398, 167, 420, 182, drawLabel="--"),
            "FB LK 3" to Button( 13, 208,  36, 226, drawLabel="--"),
            "FB RK 3" to Button(398, 208, 421, 223, drawLabel="--"),
            "FB LK 4" to Button( 12, 249,  35, 266, drawLabel="--"),
            "FB RK 4" to Button(397, 249, 420, 264, drawLabel="--"),
            "FB LK 5" to Button( 12, 293,  35, 308, drawLabel="--"),
            "FB RK 5" to Button(397, 291, 420, 306, drawLabel="--"),
            "FB LK 6" to Button( 12, 331,  35, 347, drawLabel="--"),
            "FB RK 6" to Button(397, 329, 420, 344, drawLabel="--"),
            "FB LK 7" to Button( 12, 371,  35, 386, drawLabel="--"),
            "FB RK 7" to Button(398, 368, 420, 383, drawLabel="--"),
            "FB LK 8" to Button( 13, 428,  36, 445, drawLabel="--"),
            "FB RK 8" to Button(398, 428, 420, 445, drawLabel="--"),

            "FB ON SW"  to Button(396, 37, 420, 65, drawLabel="ON"),

            "FB BRT SW+" to Button( 12, 39,  36, 61, drawLabel="BRT+"),
            "FB BRT SW-" to Button( 12, 62,  36, 83, drawLabel="BRT-"),

            "FB TOP 1" to Button( 53,  20,  90,  42, drawLabel="SND"),
            "FB TOP 2" to Button(110,  20, 148,  42, drawLabel="TOW"),
            "FB TOP 3" to Button(169,  20, 206,  42, drawLabel="PLD"),
            "FB TOP 4" to Button(226,  20, 264,  42, drawLabel="FUEL"),
            "FB TOP 5" to Button(283,  20, 320,  42, drawLabel="CLK"),
            "FB TOP 6" to Button(340,  20, 378,  42, drawLabel="OPT"),

            "FB BT 1"  to Button( 50, 500,  88, 521),
            "FB BT 2"  to Button(108, 500, 145, 521),
            "FB BT 3"  to Button(167, 500, 204, 521),
            "FB BT 4"  to Button(225, 500, 261, 521),
            "FB BT 5"  to Button(281, 500, 317, 521),
            "FB BT 6"  to Button(338, 500, 374, 521),

            "internal_help_1" to Button(  0,   0,  25,  25),
            "internal_help_2" to Button(405,   0, 432,  25),
            "internal_help_3" to Button(  0, 142,  20, 158),
            "internal_help_4" to Button(411, 139, 432, 153),
            "internal_help_5" to Button(  0, 403,  20, 420),
            "internal_help_6" to Button(411, 403, 432, 420),
            "internal_help_7" to Button(  0, 510,  25, 537),
            "internal_help_8" to Button(405, 510, 432, 537),

            "internal_display" to Button(50,  65, 382, 482)
    )

    class Dataref {
        var value: Double = -999.0
        var netvalue: Double = -999.0
        var init: Boolean = false
        var round: Boolean = false
        var array: DoubleArray? = null
        var netarray: DoubleArray? = null
    }

    val EFBDatarefsSSG748 = mapOf(
            "sim/cockpit2/switches/custom_slider_on" to Dataref(),
            "sim/FB/Clock_mode" to Dataref(),
            "sim/FB/fb_power" to Dataref(),
            "sim/FB/fb_rain" to Dataref(),
            "sim/FB/fuel_load_sw" to Dataref(),
            "sim/FB/gpu_connect" to Dataref(),
            "sim/FB/pages" to Dataref(),
            "sim/FB/pax_load_sw" to Dataref(),
            "sim/FB/payload_extra" to Dataref(),
            "sim/graphics/view/field_of_view_deg" to Dataref(),
            "sim/LGT/fb_brt_sw" to Dataref(),
            "sim/Option/p_cp" to Dataref(),
            "sim/SND/sound_master_sw" to Dataref(),
            "sim/SND/sound_Wind_sel_sw" to Dataref(),
            "sim/Yoke/hide" to Dataref(),
            "SSG/B748/CHKL/reset" to Dataref(),
            "ssg/B748/LbsKgs" to Dataref(),
            "SSG/B748/pb_call" to Dataref(),
            "SSG/B748/pb_speed_sel" to Dataref(),
            "SSG/FB/fuel_man_enter" to Dataref(),
            "SSG/FB/set_source_pred" to Dataref(),
            "SSG/UFMC/payloader_preset" to Dataref()
            // timer_start_stop and timer_reset don't appear to work in the simulator
            // "sim/instruments/timer_reset" to Dataref(),
            // "sim/instruments/timer_start_stop" to Dataref(),
    )


    // Based on definitions from https://developer.x-plane.com/article/obj8-file-format-specification/
    fun ANIM_show(v1: Double, v2: Double, dataref: String): Boolean {
        val v = getDREF(dataref)
        return ((v1 <= v) && (v <= v2))
    }

    fun ATTR_manip_toggle_button(von: Double, voff: Double, dataref: String, button: Boolean) {
        if (!button) return
        if (checkDREF(dataref, von))
            setDREF(dataref, voff)
        else
            setDREF(dataref, von)
    }

    fun ATTR_manip_radio_button(value: Double, dataref: String, button: Boolean) {
        if (!button) return
        setDREF(dataref, value)
    }

    fun ATTR_manip_axis_knob_up_down(min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        if (!button) return
        var v = getDREF(dataref)
        v += click
        if (v < min) v = min
        if (v > max) v = max
        setDREF(dataref, v)
    }

    fun _internal_ATTR_manip_axis_switch(dir: Double, min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        if (!button) return
        var value = getDREF(dataref)
        value += click * dir
        if (value < min) value = min
        if (value > max) value = max
        setDREF(dataref, value)
    }

    fun ATTR_manip_axis_switch_left_right_right(min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        _internal_ATTR_manip_axis_switch(+1.0, min, max, click, hold, dataref, button)
    }

    fun ATTR_manip_axis_switch_up_down_left(min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        _internal_ATTR_manip_axis_switch(-1.0, min, max, click, hold, dataref, button)
    }

    fun ATTR_manip_axis_switch_up_down_up(min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        _internal_ATTR_manip_axis_switch(+1.0, min, max, click, hold, dataref, button)
    }
    fun ATTR_manip_axis_switch_up_down_down(min: Double, max: Double, click: Double, hold: Double, dataref: String, button: Boolean) {
        _internal_ATTR_manip_axis_switch(-1.0, min, max, click, hold, dataref, button)
    }

    fun ATTR_manip_push_button(push: Double, release: Double, dataref: String, button: Boolean) {
        if (!button) return
        setDREF(dataref, push)
        setDREF(dataref, release)
    }


    // Build up outbound set commands to send when the execute command finishes
    private var outboundExecuteButton: String = ""
    fun executeButton(button: String) : String {
        outboundExecuteButton = ""
        // Derived from egrep "sim/FB/pages| FB " SSG_B748-I_11_cockpit.obj and reworked with these functions
        ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/fb_power", "FB ON SW" == button)
        ATTR_manip_axis_knob_up_down(0.000000, 1.000000, 0.100000, 0.500000, "sim/LGT/fb_brt_sw", "FB BRT SW+" == button)
        ATTR_manip_axis_knob_up_down(0.000000, 1.000000, -0.100000, -0.500000, "sim/LGT/fb_brt_sw", "FB BRT SW-" == button) // Extra addition
        ATTR_manip_radio_button(1.000000, "sim/FB/pages", "FB TOP 6" == button)
        ATTR_manip_radio_button(2.000000, "sim/FB/pages", "FB TOP 5" == button)
        ATTR_manip_radio_button(3.000000, "sim/FB/pages", "FB TOP 4" == button)
        ATTR_manip_radio_button(5.000000, "sim/FB/pages", "FB TOP 2" == button)
        ATTR_manip_radio_button(6.000000, "sim/FB/pages", "FB TOP 1" == button)
        ATTR_manip_radio_button(4.000000, "sim/FB/pages", "FB TOP 3" == button)
        if (ANIM_show(6.000000, 6.000000, "sim/FB/pages")) {
            ATTR_manip_radio_button(4.000000, "sim/SND/sound_master_sw", "FB RK 4" == button)
            ATTR_manip_radio_button(6.000000, "sim/SND/sound_master_sw", "FB RK 6" == button)
            ATTR_manip_axis_switch_left_right_right(0.000000, 6.000000, 1.000000, 2.000000, "sim/SND/sound_Wind_sel_sw", "FB RK 8" == button)
            ATTR_manip_axis_switch_up_down_left    (0.000000, 6.000000, 1.000000, 2.000000, "sim/SND/sound_Wind_sel_sw", "FB LK 8" == button)
            ATTR_manip_radio_button(3.000000, "sim/SND/sound_master_sw", "FB LK 7" == button)
            ATTR_manip_radio_button(2.000000, "sim/SND/sound_master_sw", "FB LK 6" == button)
            ATTR_manip_radio_button(1.000000, "sim/SND/sound_master_sw", "FB LK 5" == button)
            ATTR_manip_radio_button(0.000000, "sim/SND/sound_master_sw", "FB LK 4" == button)
        } else if (ANIM_show(5.000000, 5.000000, "sim/FB/pages")) {
            ATTR_manip_radio_button(2.000000, "SSG/B748/pb_speed_sel", "FB RK 3" == button)
            ATTR_manip_radio_button(1.000000, "SSG/B748/pb_speed_sel", "FB RK 4" == button)
            ATTR_manip_radio_button(-0.900000, "SSG/B748/pb_speed_sel", "FB RK 6" == button)
            ATTR_manip_radio_button(-1.500000, "SSG/B748/pb_speed_sel", "FB RK 7" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "SSG/B748/pb_call", "FB RK 1" == button)
        } else if (ANIM_show(4.000000, 4.000000, "sim/FB/pages")) {
            ATTR_manip_radio_button(6.000000, "SSG/UFMC/payloader_preset", "FB BT 6" == button)
            ATTR_manip_radio_button(5.000000, "SSG/UFMC/payloader_preset", "FB BT 5" == button)
            ATTR_manip_radio_button(4.000000, "SSG/UFMC/payloader_preset", "FB BT 4" == button)
            ATTR_manip_radio_button(3.000000, "SSG/UFMC/payloader_preset", "FB BT 3" == button)
            ATTR_manip_radio_button(2.000000, "SSG/UFMC/payloader_preset", "FB BT 2" == button)
            ATTR_manip_radio_button(1.000000, "SSG/UFMC/payloader_preset", "FB BT 1" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/pax_load_sw", "FB RK 1" == button)
            ATTR_manip_axis_switch_up_down_up  (0.000000, 70000.000000, 100.000000, 1000.000000, "sim/FB/payload_extra", "FB RK 2" == button)
            ATTR_manip_axis_switch_up_down_down(0.000000, 70000.000000, 100.000000, 1000.000000, "sim/FB/payload_extra", "FB RK 3" == button)
        } else if (ANIM_show(3.000000, 3.000000, "sim/FB/pages")) {
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/fuel_load_sw", "FB RK 1" == button)
            ATTR_manip_axis_switch_up_down_up  (10000.000000, 421000.000000, 1000.000000, 10000.000000, "SSG/FB/fuel_man_enter", "FB RK 2" == button)
            ATTR_manip_axis_switch_up_down_down(10000.000000, 421000.000000, 1000.000000, 10000.000000, "SSG/FB/fuel_man_enter", "FB RK 3" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "SSG/FB/set_source_pred", "FB LK 2" == button)
        } else if (ANIM_show(2.000000, 2.000000, "sim/FB/pages")) {
            // timer_start_stop and timer_reset don't appear to work in the simulator
            ATTR_manip_push_button(1.000000, 0.000000, "sim/instruments/timer_start_stop", "FB RK 3" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/instruments/timer_reset", "FB RK 2" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/Clock_mode", "FB RK 1" == button)
        } else if (ANIM_show(1.000000, 1.000000, "sim/FB/pages")) {
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/cockpit2/switches/custom_slider_on[0]", "FB RK 2" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/cockpit2/switches/custom_slider_on[1]", "FB RK 3" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/fb_rain", "FB RK 6" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "ssg/B748/LbsKgs", "FB RK 7" == button)
            ATTR_manip_push_button(1.000000, 0.000000, "SSG/B748/CHKL/reset", "FB RK 8" == button)
            ATTR_manip_radio_button(90.000000, "sim/graphics/view/field_of_view_deg", "FB LK 8" == button)
            ATTR_manip_radio_button(80.000000, "sim/graphics/view/field_of_view_deg", "FB LK 7" == button)
            ATTR_manip_radio_button(75.000000, "sim/graphics/view/field_of_view_deg", "FB LK 6" == button)
            ATTR_manip_radio_button(70.000000, "sim/graphics/view/field_of_view_deg", "FB LK 5" == button)
            ATTR_manip_radio_button(65.000000, "sim/graphics/view/field_of_view_deg", "FB LK 4" == button)
            ATTR_manip_radio_button(60.000000, "sim/graphics/view/field_of_view_deg", "FB LK 3" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/Option/p_cp", "FB LK 2" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/Yoke/hide", "FB LK 1" == button)
            ATTR_manip_toggle_button(1.000000, 0.000000, "sim/FB/gpu_connect", "FB RK 1" == button)
        }
        return outboundExecuteButton
    }
    private fun setDREF(raw: String, value: Double) {
        val result = getNameIndex(raw)
        val name = result.first
        val index = result.second

        val entry = datarefs[name]
        if (entry == null) {
            Log.e(Const.TAG, "Could not lookup dataref set request $name, so it was not declared in the mapOf")
        } else if (!entry.init) {
            Log.e(Const.TAG, "Dataref set request $name has not been received yet")
        } else if (index >= 0) {
            entry.array!![index] = value
            if (entry.round) {
                val doubles = entry.array!!
                val ints = doubles.map { it.toInt() }.toIntArray()
                outboundExecuteButton += "set $name " + ints.joinToString(prefix = "[", postfix = "]", separator = ",") + "\n"
            } else {
                outboundExecuteButton += "set $name " + entry.array!!.joinToString(prefix = "[", postfix = "]", separator = ",") + "\n"
            }
        } else {
            if (entry.round) {
                val round = value.toInt()
                outboundExecuteButton += "set $name $round\n"
            } else {
                outboundExecuteButton += "set $name $value\n"
            }
            entry.value = value
        }
    }

    private fun checkDREF(name: String, value: Double) : Boolean {
        val diff = getDREF(name) - value
        return diff.absoluteValue < 0.001
    }

    private fun getDREF(raw: String) : Double {
        val result = getNameIndex(raw)
        val name = result.first
        val index = result.second

        val entry = datarefs[name]
        if (entry == null) {
            Log.e(Const.TAG, "Could not lookup dataref get request $name, so it was not declared in the mapOf")
            return -1.0
        } else if (!entry.init) {
            Log.e(Const.TAG, "Dataref get request $name has not been received yet")
            return -1.0
        } else {
            if (index >= 0)
                return entry.array!![result.second]
            else
                return entry.value
        }
    }

    // Stores incoming dataref updates from the network thread
    fun incomingDREF(name: String, value: Double, round: Boolean) {
        val entry = datarefs[name]
        if (entry == null) {
            Log.e(Const.TAG, "Found non-EFB result name [${name} with value [$value]")
        } else {
            entry.value = value
            entry.netvalue = value
            entry.init = true
            entry.round = round
        }
    }
    fun incomingDREF(name: String, value: DoubleArray, round: Boolean) {
        val entry = datarefs[name]
        if (entry == null) {
            Log.e(Const.TAG, "Found non-EFB result name [${name} with value [$value]")
        } else {
            entry.array = value
            entry.netarray = value
            entry.init = true
            entry.round = round
        }
    }

    fun subscribeDREF(name: String): String {
        return "sub $name"
    }

    fun getNameIndex(raw: String): Pair<String, Int> {
        val stindex = raw.lastIndexOf('[')
        if (stindex < 0)
            return Pair(raw, -1)
        else {
            val endindex = raw.lastIndexOf(']')
            if (endindex < stindex) {
                Log.e(Const.TAG, "Invalid end index $endindex compared to start index $stindex from $raw")
                return Pair(raw, -1)
            } else {
                val name = raw.substring(0, stindex)
                val index = raw.substring(stindex+1, endindex)
                Log.d(Const.TAG, "Found name [$name] and index [$index] from $raw")
                return Pair(name, index.toInt())
            }
        }

    }

    var datarefs = EFBDatarefsSSG748
    var buttons = EFBButtonsSSG748

    fun reset() {
        for (entry in datarefs) {
            entry.value.init = false
            entry.value.netvalue = -999.0
            entry.value.value = -999.0
            entry.value.array = null
            entry.value.netarray = null
        }
    }
}
