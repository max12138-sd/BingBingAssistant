package com.bingbing.assistant.vision

import android.content.Context
import android.graphics.Bitmap
import com.bingbing.assistant.core.Logger
import java.io.File

/**
 * RapidOCR（PP-OCRv4 mobile）封装（§6.6）。
 * 模型：assets/models/ocr/{det,rec,cls}.onnx + keys.txt，首启解压到 filesDir/models/ocr。
 * 约束：
 *  - 只对小 ROI 调用（单次裁图 ≤ 200×80），禁止全图 OCR
 *  - 单次超时 800ms，超时返回 null 走降级
 *  - 华为无 GMS，禁用 ML Kit（§2）
 */
class OcrEngine(private val context: Context) {

    data class OcrResult(
        val text: String,
        val confidence: Float
    )

    /** 模型文件是否已就绪（决定 OCR 相关降级提示） */
    fun isModelReady(): Boolean {
        val dir = File(context.filesDir, "models/ocr")
        return listOf("det.onnx", "rec.onnx", "cls.onnx", "keys.txt").all {
            File(dir, it).length() > 0
        }
    }

    /** 首启从 assets 解压模型（无模型文件时静默跳过，等 M3 提供资产） */
    fun extractModelsIfNeeded() {
        // TODO(M3): assets/models/ocr → filesDir/models/ocr，逐文件流式复制 + 长度校验。
        Logger.i("OcrEngine", "模型解压检查（M3 实现；当前 assets 未含模型）")
    }

    /**
     * 单 ROI 识别。失败/超时/模型缺失 → null（调用方走降级，不阻塞主流程）。
     */
    suspend fun recognize(bitmap: Bitmap): OcrResult? {
        if (!isModelReady()) return null
        // TODO(M3): det（DBNet）→ 倾斜矫正 → cls（方向）→ rec（CRNN）→ CTC 解码 keys.txt
        Logger.w("OcrEngine", "OCR 尚未实现（M3），返回 null")
        return null
    }
}
