"""
Генератор анимаций техники: позы фигуры по ключевым кадрам -> app/src/main/assets/moves.json.

Фигура задаётся положением таза, наклоном корпуса и целевыми точками кистей и стоп;
локти и колени приложение достраивает обратной кинематикой (два звена).
Мир: пол y = 0, рост фигуры около 1, в профиль фигура смотрит вправо (+x).

    python3 tools/moves.py            # записать moves.json
    python3 tools/moves.py --preview  # дополнительно нарисовать кадры в tools/preview/
"""
import json
import math
import os
import sys

THIGH, SHIN, TORSO, NECK, HEAD, UPPER, FORE = 0.25, 0.25, 0.30, 0.05, 0.065, 0.17, 0.17
SHOULDER_HALF, HIP_HALF = 0.10, 0.065  # для вида спереди

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "..", "app", "src", "main", "assets", "moves.json")


def d(deg):
    r = math.radians(deg)
    return math.cos(r), math.sin(r)


def add(p, v, k=1.0):
    return (p[0] + v[0] * k, p[1] + v[1] * k)


def F(hip, torso, hands, feet, neck=None, w=1.0, bend=None, foot=None):
    f = {
        "hip": [round(hip[0], 4), round(hip[1], 4)],
        "torso": round(torso, 2),
        "hands": [[round(x, 4), round(y, 4)] for x, y in hands],
        "feet": [[round(x, 4), round(y, 4)] for x, y in feet],
    }
    if neck is not None:
        f["neck"] = neck
    if w != 1.0:
        f["w"] = w
    if bend is not None:
        f["bend"] = bend
    if foot is not None:
        f["foot"] = foot
    return f


def mirror_legs(f):
    g = json.loads(json.dumps(f))
    g["feet"] = [f["feet"][1], f["feet"][0]]
    if "foot" in f:
        g["foot"] = [f["foot"][1], f["foot"][0]]
    return g


def mirror_all(f):
    g = mirror_legs(f)
    g["hands"] = [f["hands"][1], f["hands"][0]]
    return g


def shift(f, dx=0.0, dy=0.0):
    g = json.loads(json.dumps(f))
    g["hip"] = [g["hip"][0] + dx, g["hip"][1] + dy]
    g["hands"] = [[x + dx, y + dy] for x, y in g["hands"]]
    g["feet"] = [[x + dx, y + dy] for x, y in g["feet"]]
    return g


def body_from_ankle(ankle, theta):
    """Прямое тело (планка) от щиколоток под углом theta: таз и плечи."""
    v = d(theta)
    hip = add(ankle, v, THIGH + SHIN)
    shoulder = add(hip, v, TORSO)
    return hip, shoulder


def shoulder_of(hip, torso):
    return add(hip, d(torso), TORSO)


# ---------- базовые позы ----------

def stand(hands=None, hip_y=0.495, torso=90.0, dx=0.0):
    hip = (dx, hip_y)
    sh = shoulder_of(hip, torso)
    if hands is None:
        hands = [(sh[0] + 0.03, sh[1] - 0.33), (sh[0] - 0.01, sh[1] - 0.33)]
    return F(hip, torso, hands, [(dx + 0.03, 0.0), (dx - 0.02, 0.0)])


def squat_bottom(arms="forward"):
    hip = (-0.17, 0.26)
    torso = 58.0
    sh = shoulder_of(hip, torso)
    if arms == "forward":
        hands = [(sh[0] + 0.33, sh[1] + 0.02), (sh[0] + 0.32, sh[1] - 0.0)]
    else:
        hands = arms
    return F(hip, torso, hands, [(0.04, 0.0), (0.0, 0.0)])


def high_plank(hand_x=0.0, theta=22.0, foot_lift=0.04):
    shoulder = (hand_x - 0.018, 0.34)
    v = d(theta)
    hip = add(shoulder, v, -TORSO)
    ankle = add(hip, v, -(THIGH + SHIN))
    return F(hip, theta, [(hand_x, 0.0), (hand_x - 0.02, 0.0)], [ankle, (ankle[0] - 0.02, ankle[1])])


def pushup_pair(hand_x=0.0, hand_y=0.0, top_theta=22.0, bottom_theta=6.0, ankle=None, knees=None):
    """Верх и низ отжимания: стопы (или колени) неподвижны, тело вращается вокруг них."""
    arm = UPPER + FORE - 0.005
    if knees is not None:
        pivot = knees
        length = THIGH + TORSO
    else:
        pivot = ankle
        length = THIGH + SHIN + TORSO

    def at(theta):
        v = d(theta)
        sh = add(pivot, v, length)
        hip = add(sh, v, -TORSO)
        return hip, sh

    frames = []
    for theta in (top_theta, bottom_theta):
        hip, sh = at(theta)
        if knees is not None:
            shin_dir = d(178)
            ankle_pt = add(knees, shin_dir, SHIN)
            feet = [(ankle_pt[0], ankle_pt[1] + 0.06), (ankle_pt[0] - 0.02, ankle_pt[1] + 0.06)]
        else:
            feet = [pivot, (pivot[0] - 0.02, pivot[1])]
        frames.append(F(hip, theta, [(hand_x, hand_y), (hand_x - 0.02, hand_y)], feet))
    return frames


def lying_back(hip=(0.0, 0.07), torso=180.0, hands=None, feet=None, neck=None):
    sh = shoulder_of(hip, torso)
    if hands is None:
        hands = [(sh[0] + 0.30, 0.03), (sh[0] + 0.28, 0.03)]
    if feet is None:
        feet = [(hip[0] + 0.5, 0.04), (hip[0] + 0.49, 0.04)]
    return F(hip, torso, hands, feet, neck=neck)


def lying_front(hip=(0.0, 0.07), torso=0.0, hands=None, feet=None, neck=None):
    sh = shoulder_of(hip, torso)
    if hands is None:
        hands = [(sh[0] + 0.3, 0.05), (sh[0] + 0.28, 0.05)]
    if feet is None:
        feet = [(hip[0] - 0.5, 0.05), (hip[0] - 0.49, 0.05)]
    return F(hip, torso, hands, feet, neck=neck)


# ---------- упражнения ----------

M = {}

ARMS_BACK = [-1, -1, 1, 1]   # локти назад, колени вперёд
ARMS_FWD = [1, 1, 1, 1]


def move(mid, frames, dur, props=None, bend=None, front=False):
    M[mid] = {
        "dur": dur,
        "frames": frames,
        "props": props or [],
        "bend": bend or ARMS_BACK,
        "front": front,
    }


FLOOR_MAT = [{"type": "mat", "x": -0.7, "w": 1.4}]

# --- ноги ---
move("squat", [stand(hands=[(0.05, 0.47), (0.02, 0.47)]), squat_bottom()], 2.4)

s_air = stand(hands=[(0.08, 1.24), (0.05, 1.24)], hip_y=0.62)
s_air["feet"] = [[0.05, 0.11], [0.0, 0.11]]
s_air["foot"] = [140, 140]
move("squat_jump", [squat_bottom(), F((0, 0.49), 88, [(0.25, 0.95), (0.22, 0.95)], [(0.03, 0), (-0.02, 0)]), s_air], 1.6)

sq = squat_bottom()
sq2 = shift(squat_bottom(), dy=0.012)
move("squat_hold", [sq, sq2], 3.0)

lunge = F((-0.09, 0.30), 88, [(-0.05, 0.36), (-0.09, 0.36)], [(0.10, 0.0), (-0.36, 0.05)], foot=[90, 160])
lunge_top = F((-0.02, 0.49), 90, [(0.0, 0.53), (-0.04, 0.53)], [(0.03, 0.0), (-0.12, 0.03)], foot=[90, 130])
move("reverse_lunge", [lunge_top, lunge], 2.6)

jl_a = F((-0.09, 0.32), 85, [(0.12, 0.62), (-0.25, 0.45)], [(0.12, 0.0), (-0.34, 0.06)], foot=[90, 160])
jl_air = F((-0.05, 0.56), 88, [(0.05, 0.5), (-0.05, 0.5)], [(0.0, 0.14), (-0.12, 0.12)], foot=[110, 130])
jl_b = mirror_all(jl_a)
move("jump_lunge", [jl_a, jl_air, jl_b, jl_air], 2.0)

bridge_down = lying_back(hip=(0.0, 0.07), torso=180, feet=[(0.27, 0.0), (0.25, 0.0)])
bridge_up = F((0.0, 0.25), 212, [(-0.0, 0.03), (-0.02, 0.03)], [(0.27, 0.0), (0.25, 0.0)], neck=180)
bridge_down["hands"] = [[0.03, 0.03], [0.01, 0.03]]
move("glute_bridge", [bridge_down, bridge_up], 2.4, props=FLOOR_MAT, bend=[1, 1, 1, 1])

calf_up = stand(hip_y=0.545)
calf_up["feet"] = [[0.0, 0.05], [-0.04, 0.05]]
calf_up["foot"] = [140, 140]
calf_dn = stand()
move("calf_raise", [calf_dn, calf_up], 1.8)

# --- толчок ---
BENCH = {"type": "bench", "x": 0.0, "w": 0.32, "h": 0.40}
inc = pushup_pair(hand_x=0.05, hand_y=0.40, top_theta=48, bottom_theta=36, ankle=(-0.73, 0.03))
move("incline_pushup", inc, 2.4, props=[BENCH])

knee = pushup_pair(hand_x=0.02, hand_y=0.0, top_theta=33, bottom_theta=12, knees=(-0.47, 0.035))
move("knee_pushup", knee, 2.4, props=FLOOR_MAT)

push = pushup_pair(hand_x=0.0, hand_y=0.0, top_theta=22, bottom_theta=6, ankle=(-0.76, 0.04))
move("pushup", push, 2.2, props=FLOOR_MAT)

LOW_BENCH = {"type": "bench", "x": -1.05, "w": 0.32, "h": 0.30}
decl = pushup_pair(hand_x=0.0, hand_y=0.0, top_theta=1, bottom_theta=-12, ankle=(-0.8, 0.33))
move("decline_pushup", decl, 2.4, props=[LOW_BENCH])

pike_top = F((-0.28, 0.47), -36, [(0.12, 0.0), (0.10, 0.0)], [(-0.45, 0.0), (-0.47, 0.0)])
pike_bot = F((-0.20, 0.433), -48, [(0.12, 0.0), (0.10, 0.0)], [(-0.45, 0.0), (-0.47, 0.0)])
move("pike_pushup", [pike_top, pike_bot], 2.6, props=FLOOR_MAT)

DIP_BENCH = {"type": "bench", "x": -0.42, "w": 0.38, "h": 0.30}
dip_top = F((0.03, 0.36), 95, [(-0.06, 0.31), (-0.08, 0.31)], [(0.34, 0.0), (0.32, 0.0)])
dip_bot = F((0.05, 0.17), 97, [(-0.06, 0.31), (-0.08, 0.31)], [(0.34, 0.0), (0.32, 0.0)])
move("bench_dip", [dip_top, dip_bot], 2.4, props=[DIP_BENCH])

# --- тяга ---
BAR = {"type": "bar", "x": 0.0, "y": 1.30}
hang = F((0.0, 0.66), 90, [(0.0, 1.30), (-0.02, 1.30)], [(0.03, 0.17), (0.0, 0.17)], foot=[120, 120])
hang_active = F((0.0, 0.675), 90, [(0.0, 1.30), (-0.02, 1.30)], [(0.03, 0.185), (0.0, 0.185)], foot=[120, 120])
move("dead_hang", [hang, hang_active], 2.6, props=[BAR])

pull_top = F((-0.04, 0.88), 93, [(0.0, 1.30), (-0.02, 1.30)], [(-0.0, 0.40), (-0.03, 0.40)], foot=[120, 120])
hang_s = dict(hang)
move("pullup", [hang_s, pull_top], 2.4, props=[BAR])
neg_top = dict(pull_top, w=0.6)
neg_bot = dict(hang, w=1.0)
neg_top_slow = dict(pull_top, w=3.0)
move("negative_pullup", [neg_bot, neg_top_slow], 4.0, props=[BAR])

knee_up = F((0.0, 0.67), 92, [(0.0, 1.30), (-0.02, 1.30)], [(0.18, 0.62), (0.16, 0.62)], foot=[100, 100])
move("hanging_knee_raise", [hang, knee_up], 2.4, props=[BAR])

LOW_BAR = {"type": "bar", "x": 0.0, "y": 0.75}


def row_frame(theta, hand=(0.0, 0.75), ankle=(-0.63, 0.04)):
    v = d(theta)
    sh = add(ankle, v, THIGH + SHIN + TORSO)
    hip = add(sh, v, -TORSO)
    return F(hip, theta, [hand, (hand[0] - 0.02, hand[1])], [ankle, (ankle[0] - 0.02, ankle[1])], foot=[30, 30])


row_bot = row_frame(27)
row_top = row_frame(40.5)
move("australian_row", [row_bot, row_top], 2.4, props=[LOW_BAR], bend=[-1, -1, 1, 1])

sup_down = lying_front(hip=(0.0, 0.07), torso=0.0, hands=[(0.64, 0.06), (0.62, 0.06)], feet=[(-0.5, 0.05), (-0.49, 0.05)])
sup_up = lying_front(hip=(0.0, 0.07), torso=10.0, hands=[(0.62, 0.22), (0.60, 0.21)], feet=[(-0.48, 0.17), (-0.47, 0.16)])
move("superman", [sup_down, sup_up], 2.6, props=FLOOR_MAT)

sp_reach = lying_front(hip=(0.0, 0.07), torso=10.0, hands=[(0.62, 0.21), (0.60, 0.2)], feet=[(-0.48, 0.13), (-0.47, 0.12)])
sp_pull = lying_front(hip=(0.0, 0.07), torso=12.0, hands=[(0.36, 0.20), (0.34, 0.19)], feet=[(-0.48, 0.13), (-0.47, 0.12)])
move("superman_pull", [sp_reach, sp_pull], 2.4, props=FLOOR_MAT, bend=[1, 1, 1, 1])

# --- кор ---
pl_sh = (0.0, 0.19)
pl_v = d(10.6)
pl_hip = add(pl_sh, pl_v, -TORSO)
pl_ank = add(pl_hip, pl_v, -(THIGH + SHIN))
plank = F(pl_hip, 10.6, [(0.17, 0.02), (0.15, 0.02)], [pl_ank, (pl_ank[0] - 0.02, pl_ank[1])])
plank2 = json.loads(json.dumps(plank))
plank2["hip"][1] += 0.008
move("plank", [plank, plank2], 3.0, props=FLOOR_MAT)

mc_a = high_plank(hand_x=0.1)
mc_b = json.loads(json.dumps(mc_a))
mc_a["feet"][0] = [mc_a["hip"][0] + 0.12, 0.13]
mc_b["feet"][1] = [mc_b["hip"][0] + 0.12, 0.13]
move("mountain_climber", [mc_a, mc_b], 1.0, props=FLOOR_MAT)

db_base = lying_back(hip=(0.0, 0.07), hands=[(-0.29, 0.42), (-0.31, 0.42)], feet=[(0.25, 0.32), (0.23, 0.32)])
db_ext = lying_back(hip=(0.0, 0.07), hands=[(-0.29, 0.42), (-0.62, 0.12)], feet=[(0.49, 0.1), (0.23, 0.32)])
db_ext2 = lying_back(hip=(0.0, 0.07), hands=[(-0.62, 0.12), (-0.31, 0.42)], feet=[(0.25, 0.32), (0.47, 0.1)])
move("dead_bug", [db_base, db_ext, db_base, db_ext2], 4.4, props=FLOOR_MAT, bend=[1, 1, 1, 1])

lr_down = lying_back(hip=(0.0, 0.07), feet=[(0.5, 0.08), (0.49, 0.08)])
lr_down["hands"] = [[0.03, 0.03], [0.01, 0.03]]
lr_up = lying_back(hip=(0.0, 0.07), feet=[(0.06, 0.56), (0.05, 0.56)])
lr_up["hands"] = [[0.03, 0.03], [0.01, 0.03]]
lr_up["foot"] = [180, 180]
move("leg_raises", [lr_down, lr_up], 2.8, props=FLOOR_MAT, bend=[1, 1, 1, 1])

hollow = F((0.0, 0.07), 166, [(-0.62, 0.24), (-0.6, 0.23)], [(0.49, 0.17), (0.48, 0.16)], neck=160)
hollow2 = F((0.0, 0.07), 165, [(-0.62, 0.22), (-0.6, 0.21)], [(0.49, 0.15), (0.48, 0.14)], neck=160)
move("hollow_hold", [hollow, hollow2], 3.0, props=FLOOR_MAT)

cr_down = lying_back(hip=(0.0, 0.07), feet=[(0.28, 0.0), (0.26, 0.0)])
cr_down["hands"] = [[-0.12, 0.17], [-0.14, 0.17]]
cr_up = F((0.0, 0.07), 140, [(-0.1, 0.25), (-0.12, 0.25)], [(0.28, 0.0), (0.26, 0.0)])
move("crunch", [cr_down, cr_up], 2.2, props=FLOOR_MAT, bend=[1, 1, 1, 1])

# боковая планка: вид спереди, опора на предплечье
side_plank = F((0.0, 0.194), 14, [(0.42, 0.02), (0.26, 0.73)], [(-0.47, 0.03), (-0.45, 0.09)])
side_plank_low = F((0.0, 0.15), 9, [(0.42, 0.02), (0.27, 0.69)], [(-0.47, 0.03), (-0.45, 0.09)])
move("side_plank", [side_plank, side_plank_low], 2.8, props=FLOOR_MAT, front=True, bend=[1, -1, 1, 1])



# статичные удержания для утренней статики
hp = high_plank(hand_x=0.0)
hp2 = json.loads(json.dumps(hp))
hp2["hip"][1] += 0.008
move("high_plank", [hp, hp2], 3.0, props=FLOOR_MAT)

bh2 = json.loads(json.dumps(bridge_up))
bh2["hip"][1] -= 0.01
move("bridge_hold", [bridge_up, bh2], 3.0, props=FLOOR_MAT, bend=[1, 1, 1, 1])


# --- кардио и разминка ---
jj_in = F((0.0, 0.502), 90, [(0.09, 0.47), (-0.09, 0.47)], [(0.07, 0.0), (-0.07, 0.0)])
jj_out = F((0.0, 0.52), 90, [(0.30, 1.12), (-0.30, 1.12)], [(0.24, 0.04), (-0.24, 0.04)])
move("jumping_jacks", [jj_in, jj_out], 1.0, front=True, bend=[1, -1, 1, -1])

sj_in = jj_in
sj_out_a = F((0.0, 0.502), 90, [(0.30, 1.12), (-0.30, 1.12)], [(0.30, 0.0), (-0.07, 0.0)])
sj_out_b = F((0.0, 0.502), 90, [(0.30, 1.12), (-0.30, 1.12)], [(0.07, 0.0), (-0.30, 0.0)])
move("step_jacks", [sj_in, sj_out_a, sj_in, sj_out_b], 2.4, front=True, bend=[1, -1, 1, -1])

ac = []
for k in range(8):
    a = k * 45
    r = 0.07
    ac.append(F((0.0, 0.502), 90, [(0.42 + r * math.cos(math.radians(a)), 0.78 + r * math.sin(math.radians(a))),
                                    (-0.42 - r * math.cos(math.radians(a)), 0.78 + r * math.sin(math.radians(a)))],
                [(0.07, 0.0), (-0.07, 0.0)]))
move("arm_circles", ac, 1.6, front=True, bend=[1, -1, 1, -1])


def knee_lift(height, hip_y, lift, arm):
    hip = (0.0, hip_y)
    a = F(hip, 88, [(-0.13, 0.52 + arm * 0.2), (0.16, 0.62 + arm)], [(0.17, height), (-0.01, lift)], foot=[90, 90])
    return a


hk_a = knee_lift(0.30, 0.52, 0.03, 0.12)
hk_b = mirror_all(hk_a)
move("high_knees", [hk_a, hk_b], 0.7)

march_a = knee_lift(0.22, 0.495, 0.0, 0.0)
march_b = mirror_all(march_a)
move("march", [march_a, march_b], 1.4)

walk_c = F((0.0, 0.47), 88, [(-0.12, 0.50), (0.14, 0.52)], [(0.18, 0.0), (-0.18, 0.02)], foot=[90, 120])
walk_p = F((0.0, 0.50), 88, [(0.01, 0.47), (0.0, 0.47)], [(0.0, 0.0), (0.04, 0.12)], foot=[90, 100])
move("walk", [walk_c, walk_p, mirror_all(walk_c), mirror_all(walk_p)], 1.4)

run_c = F((0.0, 0.47), 80, [(-0.06, 0.48), (0.2, 0.68)], [(0.14, 0.0), (-0.2, 0.18)], foot=[90, 140])
run_p = F((0.0, 0.45), 80, [(0.06, 0.56), (0.0, 0.54)], [(-0.02, 0.0), (0.1, 0.24)], foot=[90, 100])
run_f = F((0.0, 0.52), 80, [(0.2, 0.68), (-0.06, 0.48)], [(-0.18, 0.12), (0.18, 0.1)], foot=[130, 100])
move("run", [run_c, run_p, run_f, mirror_all(run_p)], 0.8)

burpee_stand = stand()
burpee_sq = F((-0.12, 0.24), 40, [(0.2, 0.0), (0.18, 0.0)], [(0.02, 0.0), (-0.01, 0.0)])
burpee_plank = high_plank(hand_x=0.2)
burpee_jump = stand(hands=[(0.07, 1.22), (0.04, 1.22)], hip_y=0.6)
burpee_jump["feet"] = [[0.03, 0.1], [-0.02, 0.1]]
burpee_jump["foot"] = [140, 140]
move("burpee", [burpee_stand, burpee_sq, burpee_plank, burpee_sq, burpee_jump], 3.4, props=FLOOR_MAT)

step_back = json.loads(json.dumps(burpee_plank))
step_back["feet"][1] = [burpee_sq["feet"][1][0] - 0.05, 0.05]
step_back["hip"] = [step_back["hip"][0] + 0.06, step_back["hip"][1] + 0.08]
step_back["torso"] = 30
move("burpee_low", [burpee_stand, burpee_sq, step_back, burpee_plank, step_back, burpee_sq, burpee_stand], 6.0,
     props=FLOOR_MAT)

iw_stand = stand()
iw_fold = F((-0.10, 0.50), -80, [(0.1, 0.0), (0.08, 0.0)], [(0.02, 0.0), (-0.01, 0.0)])
iw_mid = F((-0.14, 0.42), -35, [(0.36, 0.0), (0.34, 0.0)], [(0.02, 0.0), (-0.01, 0.0)])
iw_plank = high_plank(hand_x=0.76)
move("inchworm", [iw_stand, iw_fold, iw_mid, iw_plank, iw_mid, iw_fold], 6.0, props=FLOOR_MAT)

# --- растяжка ---
ham_a = F((0.0, 0.08), 92, [(0.08, 0.42), (0.05, 0.42)], [(0.5, 0.03), (0.49, 0.03)], foot=[180, 180])
ham_b = F((0.0, 0.08), 38, [(0.46, 0.08), (0.44, 0.08)], [(0.5, 0.03), (0.49, 0.03)], foot=[180, 180])
move("hamstring_stretch", [ham_a, dict(ham_b, w=2.0)], 5.0, props=FLOOR_MAT)

child_a = F((-0.10, 0.17), -6, [(0.55, 0.03), (0.53, 0.03)], [(-0.17, 0.03), (-0.18, 0.03)], neck=-25, foot=[180, 180])
child_b = F((-0.12, 0.15), -9, [(0.55, 0.03), (0.53, 0.03)], [(-0.17, 0.03), (-0.18, 0.03)], neck=-25, foot=[180, 180])
move("child_pose", [child_a, child_b], 4.0, props=FLOOR_MAT, bend=[1, 1, 1, 1])

cobra_a = lying_front(hip=(0.0, 0.06), torso=4.0, hands=[(0.24, 0.02), (0.22, 0.02)])
cobra_b = lying_front(hip=(0.0, 0.06), torso=38.0, hands=[(0.24, 0.02), (0.22, 0.02)], neck=60)
move("cobra", [cobra_a, dict(cobra_b, w=2.0)], 5.0, props=FLOOR_MAT)

quad = F((0.0, 0.495), 90, [(0.05, 0.47), (-0.14, 0.36)], [(0.03, 0.0), (-0.16, 0.36)], foot=[90, 270], bend=[-1, -1, 1, 1])
quad2 = json.loads(json.dumps(quad))
quad2["torso"] = 92
move("quad_stretch", [quad, quad2], 4.0)


# ---------- кинематика, общая с приложением ----------

def ik(root, target, l1, l2, s):
    dx, dy = target[0] - root[0], target[1] - root[1]
    dist = math.hypot(dx, dy)
    dist_c = max(abs(l1 - l2) + 1e-4, min(l1 + l2 - 1e-4, dist))
    base = math.atan2(dy, dx)
    cos_a = (l1 * l1 + dist_c * dist_c - l2 * l2) / (2 * l1 * dist_c)
    a = math.acos(max(-1.0, min(1.0, cos_a)))
    joint = (root[0] + l1 * math.cos(base + s * a), root[1] + l1 * math.sin(base + s * a))
    end = (root[0] + dist_c * math.cos(base), root[1] + dist_c * math.sin(base))
    return joint, end


def solve(frame, mv):
    """Точки скелета для кадра (та же логика, что в Figure.kt)."""
    hip = tuple(frame["hip"])
    t = frame["torso"]
    bend = frame.get("bend", mv["bend"])
    v = d(t)
    sh = add(hip, v, TORSO)
    if mv["front"]:
        perp = (math.cos(math.radians(t - 90)), math.sin(math.radians(t - 90)))
        sh_pts = [add(sh, perp, SHOULDER_HALF), add(sh, perp, -SHOULDER_HALF)]
        hip_pts = [add(hip, perp, HIP_HALF), add(hip, perp, -HIP_HALF)]
    else:
        sh_pts = [sh, sh]
        hip_pts = [hip, hip]
    neck_ang = frame.get("neck", t)
    neck = add(sh, d(neck_ang), NECK)
    head = add(neck, d(neck_ang), HEAD)
    arms, legs = [], []
    for i in range(2):
        e, h = ik(sh_pts[i], frame["hands"][i], UPPER, FORE, bend[i])
        arms.append((sh_pts[i], e, h))
        k, a = ik(hip_pts[i], frame["feet"][i], THIGH, SHIN, bend[2 + i])
        legs.append((hip_pts[i], k, a))
    return {"hip": hip, "sh": sh, "neck": neck, "head": head, "arms": arms, "legs": legs,
            "sh_pts": sh_pts, "hip_pts": hip_pts}


def foot_tip(knee, ankle, rot):
    ang = math.atan2(ankle[1] - knee[1], ankle[0] - knee[0]) + math.radians(rot)
    return (ankle[0] + 0.07 * math.cos(ang), ankle[1] + 0.07 * math.sin(ang))


def bounds(mv):
    xs, ys = [], []
    for f in mv["frames"]:
        s = solve(f, mv)
        pts = [s["head"], s["neck"]] + [p for a in s["arms"] for p in a] + [p for l in s["legs"] for p in l]
        for i, l in enumerate(s["legs"]):
            rot = f.get("foot", [90, 90])[i]
            pts.append(foot_tip(l[1], l[2], rot))
        for x, y in pts:
            xs.append(x)
            ys.append(y)
        xs += [s["head"][0] - HEAD, s["head"][0] + HEAD]
        ys += [s["head"][1] + HEAD]
    for p in mv["props"]:
        if p["type"] == "bar":
            xs += [p["x"] - 0.05, p["x"] + 0.05]
            ys += [p["y"] + 0.05]
        elif p["type"] in ("bench", "mat"):
            xs += [p["x"], p["x"] + p["w"]]
            ys += [p.get("h", 0.0)]
    ys.append(0.0)
    return [round(min(xs), 3), round(min(ys), 3), round(max(xs), 3), round(max(ys), 3)]


def preview(mid, mv, path):
    from PIL import Image, ImageDraw
    n = len(mv["frames"])
    W, H = 260, 260
    img = Image.new("RGB", (W * n, H), (15, 17, 21))
    dr = ImageDraw.Draw(img)
    b = bounds(mv)
    bw, bh = b[2] - b[0], b[3] - b[1]
    sc = min((W - 30) / bw, (H - 30) / bh)
    for i, f in enumerate(mv["frames"]):
        ox = i * W + (W - bw * sc) / 2 - b[0] * sc
        oy = H - (H - bh * sc) / 2 + b[1] * sc

        def P(p):
            return (ox + p[0] * sc, oy - p[1] * sc)

        dr.line([P((b[0], 0)), P((b[2], 0))], fill=(58, 64, 80), width=2)
        for p in mv["props"]:
            if p["type"] == "bar":
                dr.ellipse([P((p["x"] - 0.03, p["y"] + 0.03)), P((p["x"] + 0.03, p["y"] - 0.03))], fill=(150, 160, 175))
            elif p["type"] == "bench":
                dr.rectangle([P((p["x"], p["h"])), P((p["x"] + p["w"], 0))], outline=(90, 100, 120), width=2)
        s = solve(f, mv)
        lw = int(0.05 * sc)
        for idx, col in ((1, (150, 80, 40)), (0, (255, 138, 61))):
            if idx == 0:
                dr.line([P(s["hip"]), P(s["sh"])], fill=(255, 160, 90), width=int(0.08 * sc))
                dr.line([P(s["sh"]), P(s["neck"])], fill=(255, 160, 90), width=lw)
                hx, hy = P(s["head"])
                r = HEAD * sc
                dr.ellipse([hx - r, hy - r, hx + r, hy + r], fill=(255, 160, 90))
            a = s["arms"][idx]
            l = s["legs"][idx]
            dr.line([P(a[0]), P(a[1]), P(a[2])], fill=col, width=lw, joint="curve")
            rot = f.get("foot", [90, 90])[idx]
            dr.line([P(l[0]), P(l[1]), P(l[2]), P(foot_tip(l[1], l[2], rot))], fill=col, width=lw, joint="curve")
        dr.text((i * W + 6, 6), f"{mid} {i}", fill=(200, 200, 200))
    img.save(path)


def main():
    out = {}
    for mid, mv in M.items():
        mv = dict(mv)
        mv["box"] = bounds(mv)
        out[mid] = mv
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump({"moves": out}, fh, ensure_ascii=False, separators=(",", ":"))
    print(f"{len(out)} движений -> {os.path.relpath(OUT)}")
    if "--preview" in sys.argv:
        pdir = sys.argv[sys.argv.index("--preview") + 1] if len(sys.argv) > sys.argv.index("--preview") + 1 else os.path.join(ROOT, "preview")
        os.makedirs(pdir, exist_ok=True)
        for mid, mv in out.items():
            preview(mid, mv, os.path.join(pdir, f"{mid}.png"))


if __name__ == "__main__":
    main()
