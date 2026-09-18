import os

BASE = "src/main/resources/assets/createnuclearized/models/block/turbines"
SIZES = (3, 5, 7)


def is_rotor(name):
    return (name == "Shaft" or name == "Hub_core"
            or name.startswith("Root_socket")
            or name.startswith("Root_bolt")
            or name.startswith("Blade"))


def split(n):
    path = f"{BASE}/turbine_{n}/turbine_{n}.obj"
    if not os.path.exists(path):
        print(f"  skip turbine_{n}: {path} not found")
        return
    lines = open(path, encoding="utf-8").read().splitlines()
    header, vlines, vnlines, vtlines, objs = [], [], [], [], []
    cur = None
    seen_o = False
    for ln in lines:
        s = ln.split()
        if not s:
            continue
        k = s[0]
        if k == 'o':
            seen_o = True
            cur = []
            objs.append((s[1], cur))
            cur.append(ln)
        elif k == 'v':
            vlines.append(ln)
        elif k == 'vn':
            vnlines.append(ln)
        elif k == 'vt':
            vtlines.append(ln)
        elif not seen_o and k in ('#', 'mtllib'):
            header.append(ln)
        elif cur is not None:
            # usemtl / s / f and anything else that belongs to the current object
            cur.append(ln)
        else:
            header.append(ln)

    def write(select, suffix):
        # Keep the whole v/vn/vt pool in both files so the original (global, cumulative) face indices stay valid without re indexing
        out = list(header) + vlines + vnlines + vtlines
        kept = 0
        for name, block in objs:
            if select(name):
                out += block
                kept += 1
        dst = f"{BASE}/turbine_{n}/turbine_{n}_{suffix}.obj"
        open(dst, "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
        print(f"  wrote {os.path.basename(dst)} ({kept} objects)")

    print(f"turbine_{n}: {len(objs)} objects")
    write(is_rotor, "rotor")
    write(lambda nm: not is_rotor(nm), "static")


if __name__ == "__main__":
    for n in SIZES:
        split(n)
