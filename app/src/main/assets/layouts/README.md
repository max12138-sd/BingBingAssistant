# layouts 目录说明（§6.3 布局配置外置）

文件命名：`{界面类型}_{宽}x{高}.json`，运行时按**宽高比**选最接近的配置（DeviceProfile.pickClosest）。

- `arena_*.json`：竞技场对手阵容页（oppSlots / mySlots 各 5 槽）
- `peak3_*.json` / `peak5_*.json`：巅峰布阵页（myTeams / oppTeams 各 3/5 组 × 5 槽）
- `calendar_*.json`：活动日历页（monthHeaderRoi / gridRoi / legendRoi 三个 ROI）

坐标全部是**归一化值 [x, y, w, h]**（0..1，相对整屏），禁止硬编码像素。

⚠ 当前所有 JSON 内的坐标是**占位值**（TODO(M3/M4/M5)）：
上架前必须用真机截图校准 —— 打开对应界面截图，量出各卡槽/ROI 的归一化矩形后替换。
校准工具建议：M3 实现时在设置页加"布局校准"入口（截屏叠加网格线辅助标定）。
