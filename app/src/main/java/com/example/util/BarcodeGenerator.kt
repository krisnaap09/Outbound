package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-precision Code 128 (Subset B) barcode generator for logistics & warehouse bagseal tracking.
 * Encodes ASCII 32..126 into deterministic 1D bar/space patterns readable by any handheld PDA gun,
 * laser terminal, or smartphone scanner camera.
 */
object Code128Encoder {

  // Patterns for Code 128 symbols 0..106
  // Each integer bitfield represents 11 bits (or 13 for stop code)
  private val PATTERNS = intArrayOf(
    0x6cc, 0x66c, 0x666, 0x498, 0x48c, 0x44c, 0x4c8, 0x4c4, 0x464, 0x648, // 0-9
    0x644, 0x624, 0x59c, 0x4dc, 0x4ce, 0x5c9, 0x4c9, 0x471, 0x670, 0x638, // 10-19
    0x61c, 0x68e, 0x647, 0x631, 0x5b0, 0x598, 0x58c, 0x586, 0x5ac, 0x5a6, // 20-29
    0x54e, 0x547, 0x527, 0x730, 0x718, 0x70c, 0x738, 0x71c, 0x70e, 0x6b0, // 30-39
    0x698, 0x68c, 0x6b8, 0x69c, 0x68e, 0x62e, 0x617, 0x60b, 0x3b0, 0x398, // 40-49
    0x38c, 0x3b8, 0x39c, 0x38e, 0x560, 0x538, 0x51c, 0x568, 0x53c, 0x51e, // 50-59
    0x570, 0x538, 0x51c, 0x4b0, 0x498, 0x48c, 0x4b8, 0x49c, 0x48e, 0x2b0, // 60-69
    0x298, 0x28c, 0x2b8, 0x29c, 0x28e, 0x52e, 0x517, 0x50b, 0x720, 0x710, // 70-79
    0x708, 0x6a0, 0x690, 0x688, 0x6a8, 0x694, 0x68a, 0x2a0, 0x290, 0x288, // 80-89
    0x2a8, 0x294, 0x28a, 0x4a0, 0x490, 0x488, 0x4a8, 0x494, 0x48a, 0x6e0, // 90-99
    0x6d0, 0x6c8, 0x6e8, 0x6d4, 0x6ca, 0x67c, 0x18eb // 100-106 (104=StartB, 106=Stop)
  )

  private const val START_CODE_B = 104
  private const val STOP_CODE = 106

  /**
   * Encodes a string into a boolean array where true = bar (black), false = space (white).
   */
  fun encodeToBitArray(content: String): BooleanArray {
    val clean = if (content.isBlank()) "000000" else content.trim()
    val symbolIndices = mutableListOf<Int>()
    symbolIndices.add(START_CODE_B)

    var checksum = START_CODE_B
    for (i in clean.indices) {
      val charCode = clean[i].code
      val code128Val = if (charCode in 32..126) charCode - 32 else 0
      symbolIndices.add(code128Val)
      checksum += code128Val * (i + 1)
    }

    val checkSymbol = checksum % 103
    symbolIndices.add(checkSymbol)
    symbolIndices.add(STOP_CODE)

    val bits = mutableListOf<Boolean>()
    // Quiet zone (10 modules)
    repeat(10) { bits.add(false) }

    for (symbol in symbolIndices) {
      val bitLength = if (symbol == STOP_CODE) 13 else 11
      val pattern = PATTERNS.getOrElse(symbol) { PATTERNS[0] }
      for (b in bitLength - 1 downTo 0) {
        val isBar = ((pattern shr b) and 1) == 1
        bits.add(isBar)
      }
    }

    // Quiet zone (10 modules)
    repeat(10) { bits.add(false) }
    return bits.toBooleanArray()
  }
}

@Composable
fun BarcodeView(
  barcodeText: String,
  modifier: Modifier = Modifier,
  showLabel: Boolean = true,
  barColor: Color = Color.Black,
  backgroundColor: Color = Color.White,
  onClick: (() -> Unit)? = null
) {
  val bitArray = remember(barcodeText) {
    Code128Encoder.encodeToBitArray(barcodeText)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .background(backgroundColor, RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    ) {
      if (bitArray.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas
      val totalModules = bitArray.size
      val moduleWidth = size.width / totalModules.toFloat()
      val canvasHeight = size.height

      // Clear background
      drawRect(
        color = backgroundColor,
        topLeft = Offset.Zero,
        size = size
      )

      for (i in bitArray.indices) {
        if (bitArray[i]) {
          drawRect(
            color = barColor,
            topLeft = Offset(i * moduleWidth, 0f),
            size = Size(moduleWidth + 0.5f, canvasHeight)
          )
        }
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = barcodeText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = barColor,
        letterSpacing = 1.5.sp
      )
    }
  }
}
