# 冰冰助手 R8 规则
# 原则：release 开混淆，但视觉库与数据模型按白名单保留，防止反射解析失败。

# ---- ONNX Runtime（JNI 按类名桥接）----
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ---- OpenCV（JNI 桥接）----
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ---- Room 实体 / CSV-JSON 导入模型：保留字段名（Gson/手工 JSON 解析按字段名）----
-keep class com.bingbing.assistant.data.entity.** { *; }
-keep class com.bingbing.assistant.data.import.** { *; }

# ---- 布局配置 / 种子数据反序列化 ----
-keep class com.bingbing.assistant.vision.LayoutProfile { *; }
-keep class com.bingbing.assistant.data.import.SeedModels$* { *; }

-keepattributes Signature
-keepattributes *Annotation*
