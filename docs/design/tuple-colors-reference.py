"""Proposed tuple-color algorithm: standalone design reference, not app code.
Run with Python 3; no external packages. Constants remain a design proposal.
"""
import hashlib
import math

def component(tag):
    digest=hashlib.sha256(b'muh-todo:tuple-color:v1\x00'+tag.encode('utf-8')).digest()
    return [(int.from_bytes(digest[i:i+8], 'big') >> 11)/2**53 for i in range(0,32,8)]

def weight(i):
    return [1.,.35,.15][i] if i<3 else .07*.5**(i-3)

def linear_rgb(L,C,h):
    h=math.radians(h)
    a=C*math.cos(h);b=C*math.sin(h)
    ll=L+.3963377774*a+.2158037573*b
    mm=L-.1055613458*a-.0638541728*b
    ss=L-.0894841775*a-1.2914855480*b
    ll=ll**3;mm=mm**3;ss=ss**3
    return (4.0767416621*ll-3.3077115913*mm+.2309699292*ss,
        -1.2684380046*ll+2.6097574011*mm-.3413193965*ss,
        -.0041960863*ll-.7034186147*mm+1.7076147010*ss)

def cmax(L,h):
    lo,hi=0.,.4
    for _ in range(24):
        mid=(lo+hi)/2
        if all(0<=v<=1 for v in linear_rgb(L,mid,h)):lo=mid
        else:hi=mid
    return lo

def distance(a,b):return math.dist(a['lab'],b['lab'])

def luminance(rgb8):
    s=[x/255 for x in rgb8]
    ls=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in s]
    return sum(a*b for a,b in zip(ls,(.2126,.7152,.0722)))

def contrast(c,bg=(27,30,35)):return (luminance(c['rgb8'])+.05)/(luminance(bg)+.05)

def digest_units(d):return [(int.from_bytes(d[i:i+8],'big') >> 11)/2**53 for i in range(0,32,8)]

def numeric_value(tag):
 import unicodedata
 if len(tag)<2:return None
 ds=[unicodedata.decimal(x,None) for x in tag[1:]]
 if any(x is None for x in ds):return None
 n=0
 for x in ds:n=min(99,n*10+x)
 return n

def color(tags):
 numeric=[(i,t,numeric_value(t)) for i,t in enumerate(tags) if numeric_value(t) is not None]
 # First numeric component is priority; extra numeric components are preserved, not summed.
 priority=numeric[0][2] if numeric else 0
 p=priority/99
 L=.74+.08*p;q=.62+.24*p;h=0;root=None;depth=0
 seed=hashlib.sha256(b'muh-todo:semantic-path:v1').digest()
 for i,t in enumerate(tags):
  u=component(t);v=[2*x-1 for x in u];n=numeric_value(t)
  if n is not None:
   # Preserve numeric ordering/spelling without choosing a hue family.
   h+=2*weight(i)*v[1]
   continue
  raw=hashlib.sha256(b'muh-todo:tuple-color:v1\x00'+t.encode('utf-8')).digest()
  seed=hashlib.sha256(seed+raw).digest()
  if root is None:
   root=360*u[0]
   # Root variation is independent of numeric priority; ordered numeric offsets remain in hue.
   L+=.008*v[2];q+=.03*v[3]
  else:
   m=[2*x-1 for x in digest_units(seed)];w=weight(depth)
   magnitude=.65+.35*(m[0]+1)/2
   length=math.sqrt(sum(v*v for v in m[1:]))
   direction=[1.,0.,0.] if length == 0 else [v/length for v in m[1:]]
   h+=90*w*magnitude*direction[0]
   L+=.05*w*magnitude*direction[1]
   q+=.20*w*magnitude*direction[2]
  depth+=1
 L=min(.88,max(.70,L));q=min(.95,max(.45,q))
 h=((250 if root is None else root)+h)%360
 C=(.018+.022*p) if root is None else q*cmax(L,h)
 rgb=linear_rgb(L,C,h)
 srgb=tuple(12.92*x if x<=.0031308 else 1.055*x**(1/2.4)-.055 for x in rgb)
 rgb8=tuple(round(min(1,max(0,x))*255) for x in srgb)
 return dict(tags=list(tags),priority=priority,L=L,C=C,h=h,relative_chroma=q,rgb8=rgb8,hex='#'+''.join(f'{x:02x}' for x in rgb8),lab=(L,C*math.cos(math.radians(h)),C*math.sin(math.radians(h))))

def with_priority(semantic,value):return ([f'#{value}'] if value else [])+list(semantic)

def check():
    import json
    examples = [
        ["#3"], ["#3", "#belongings"],
        ["#3", "#belongings", "#relocation"],
        ["#3", "#belongings", "#storage"],
        ["#3", "#relocation", "#belongings"], ["#belongings", "#3"],
        ["#3", "#belongings", "#relocation", "#packing"],
        ["#3", "#belongings", "#relocation", "#delivery"],
    ] + [with_priority(["#belongings", "#relocation"], n)
         for n in [0,10,20,30,35,40,50,60,69,70,80,90,99]]
    words = ["relocation", "storage", "packing", "delivery", "furniture",
             "documents", "clothes", "books", "electronics", "kitchen", "tools", "garden"]
    siblings = [color(["#3", "#belongings", "#" + w]) for w in words]
    parent = color(["#3", "#belongings"])
    results = [color(t) for t in examples]
    colors = results + siblings + [color(with_priority([f"#tag{i}", f"#branch{i%37}"], i%100)) for i in range(4000)]
    for c in colors:
        assert all(-1e-12 <= x <= 1+1e-12 for x in linear_rgb(c["L"], c["C"], c["h"]))
        assert contrast(c) >= 4.5
    for n in range(100):
        levels = [color(with_priority([f"#tag{n}", "#child"], v))["L"] for v in range(100)]
        assert levels == sorted(levels)
    assert results[2]["hex"] != results[4]["hex"]
    assert results[1]["hex"] != results[5]["hex"]
    assert color(["#100", "#belongings"])["priority"] == 99
    assert color(["#٠٣", "#belongings"])["priority"] == 3
    assert color(["#3.5", "#belongings"])["priority"] == 0
    assert color(["#3", "#9", "#belongings"])["priority"] == 3
    distances = [distance(a,b) for i,a in enumerate(siblings) for b in siblings[:i]]
    return {
        "sampled_colors": len(colors),
        "minimum_dark_contrast": min(contrast(c) for c in colors),
        "parent_child_distance_range": [min(distance(parent,c) for c in siblings), max(distance(parent,c) for c in siblings)],
        "sibling_distance_range": [min(distances), max(distances)],
        "sibling_pairs_below_002": sum(d < .02 for d in distances),
        "sibling_pairs": len(distances),
        "example_sibling_distance": distance(results[2],results[3]),
        "examples": [{"tuple": c["tags"], "color": c["hex"]} for c in results[:8]],
    }

if __name__ == "__main__":
    import json
    print(json.dumps(check(), ensure_ascii=False, indent=2))
