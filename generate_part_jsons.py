import json, os

parts = {
    "brick": (8,16,8),
    "cube": (8,8,8),
    "small_slab": (8,4,8),
    "small_brick": (4,8,4),
    "small_cube": (4,4,4),
    "plate": (16,4,16),
    "tile": (16,4,8),
    "rod": (4,16,4)
}

base_blockstates = 'src/main/resources/assets/blockparts/blockstates'
base_models = 'src/main/resources/assets/blockparts/models/block'
os.makedirs(base_blockstates, exist_ok=True)
os.makedirs(base_models, exist_ok=True)

for name, (dx, dy, dz) in parts.items():
    blockstate = {
        "variants": {
            "axis=x": {"model": f"blockparts:block/{name}_x"},
            "axis=y": {"model": f"blockparts:block/{name}_y"},
            "axis=z": {"model": f"blockparts:block/{name}_z"}
        }
    }
    with open(os.path.join(base_blockstates, f"{name}.json"), 'w') as f:
        json.dump(blockstate, f, indent=2)

    axes = {
        'y': (dx, dy, dz),
        'x': (dy, dx, dz),
        'z': (dx, dz, dy)
    }

    for axis, (sx, sy, sz) in axes.items():
        model = {
            "parent": "block/block",
            "textures": {
                "particle": "minecraft:block/stone",
                "down": "minecraft:block/stone",
                "up": "minecraft:block/stone",
                "north": "minecraft:block/stone",
                "south": "minecraft:block/stone",
                "west": "minecraft:block/stone",
                "east": "minecraft:block/stone"
            },
            "elements": [
                {
                    "from": [0,0,0],
                    "to": [sx, sy, sz],
                    "faces": {
                        "down":  {"texture": "#down"},
                        "up":    {"texture": "#up"},
                        "north": {"texture": "#north"},
                        "south": {"texture": "#south"},
                        "west":  {"texture": "#west"},
                        "east":  {"texture": "#east"}
                    }
                }
            ]
        }
        with open(os.path.join(base_models, f"{name}_{axis}.json"), 'w') as f:
            json.dump(model, f, indent=2)
