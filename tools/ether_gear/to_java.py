"""Escribe las piezas de spec.py dentro de EtherGearModel.createBodyLayer (python3 to_java.py)."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from spec import PARTS
def f(x):
    s = ('%.3f' % x).rstrip('0').rstrip('.')
    return (s if s not in ('-0', '') else '0') + 'F'
lines = []; var = {None: 'root'}
for name, parent, pivot, rot, boxes in PARTS:
    cubes = ''
    for bx in boxes:
        (u, v, x, y, z, w, h, d) = bx[:8]; m = len(bx) > 8 and bx[8]
        cubes += f'.texOffs({u}, {v})' + ('.mirror()' if m else '') + f'.addBox({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)})'
    rx, ry, rz = rot
    pose = (f'PartPose.offsetAndRotation({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])}, {f(rx * 3.14159265 / 180)}, '
            f'{f(ry * 3.14159265 / 180)}, {f(rz * 3.14159265 / 180)})') if any(rot) else f'PartPose.offset({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])})'
    v = name.replace('_', '')
    lines.append(f'        PartDefinition {v} = {var[parent]}.addOrReplaceChild("{name}", CubeListBuilder.create(){cubes},\n                {pose});')
    var[name] = v
J = os.path.join(os.path.dirname(__file__), '../../src/main/java/com/markadrii/draviti/client/ethergear/EtherGearModel.java')
s = open(J).read()
a = s.index('        PartDefinition body ='); b = s.index('        return LayerDefinition.create')
open(J, 'w').write(s[:a] + '\n'.join(lines) + '\n' + s[b:])
print(len(lines), 'piezas')
