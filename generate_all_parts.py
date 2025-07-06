import os, json

# shapes and their base geometry names
PARTS = {
    "brick": (8,16,8),
    "cube": (8,8,8),
    "small_slab": (8,4,8),
    "small_brick": (4,8,4),
    "small_cube": (4,4,4),
    "plate": (16,4,16),
    "tile": (16,4,8),
    "rod": (4,16,4)
}

SLABS = {
    "oak_slab": "oak_planks",
    "spruce_slab": "spruce_planks",
    "birch_slab": "birch_planks",
    "jungle_slab": "jungle_planks",
    "acacia_slab": "acacia_planks",
    "dark_oak_slab": "dark_oak_planks",
    "mangrove_slab": "mangrove_planks",
    "cherry_slab": "cherry_planks",
    "bamboo_slab": "bamboo_planks",
    "bamboo_mosaic_slab": "bamboo_mosaic",
    "crimson_slab": "crimson_planks",
    "warped_slab": "warped_planks",
    "stone_slab": "stone",
    "smooth_stone_slab": "smooth_stone",
    "cobblestone_slab": "cobblestone",
    "mossy_cobblestone_slab": "mossy_cobblestone",
    "stone_brick_slab": "stone_bricks",
    "mossy_stone_brick_slab": "mossy_stone_bricks",
    "brick_slab": "bricks",
    "sandstone_slab": "sandstone_top",
    "cut_sandstone_slab": "sandstone_top",
    "smooth_sandstone_slab": "smooth_sandstone",
    "red_sandstone_slab": "red_sandstone_top",
    "cut_red_sandstone_slab": "red_sandstone_top",
    "smooth_red_sandstone_slab": "smooth_red_sandstone",
    "purpur_slab": "purpur_block",
    "prismarine_slab": "prismarine_bricks",
    "prismarine_brick_slab": "prismarine_bricks",
    "dark_prismarine_slab": "dark_prismarine",
    "nether_brick_slab": "nether_bricks",
    "red_nether_brick_slab": "red_nether_bricks",
    "quartz_slab": "quartz_block_top",
    "smooth_quartz_slab": "quartz_block_top",
    "polished_granite_slab": "polished_granite",
    "granite_slab": "granite",
    "polished_diorite_slab": "polished_diorite",
    "diorite_slab": "diorite",
    "polished_andesite_slab": "polished_andesite",
    "andesite_slab": "andesite",
    "copper_slab": "copper_block",
    "exposed_copper_slab": "exposed_copper",
    "weathered_copper_slab": "weathered_copper",
    "oxidized_copper_slab": "oxidized_copper",
    "waxed_copper_slab": "waxed_copper",
    "waxed_exposed_copper_slab": "waxed_exposed_copper",
    "waxed_weathered_copper_slab": "waxed_weathered_copper",
    "waxed_oxidized_copper_slab": "waxed_oxidized_copper",
    "cut_copper_slab": "cut_copper",
    "exposed_cut_copper_slab": "exposed_cut_copper",
    "weathered_cut_copper_slab": "weathered_cut_copper",
    "oxidized_cut_copper_slab": "oxidized_cut_copper",
    "waxed_cut_copper_slab": "waxed_cut_copper",
    "waxed_exposed_cut_copper_slab": "waxed_exposed_cut_copper",
    "waxed_weathered_cut_copper_slab": "waxed_weathered_cut_copper",
    "waxed_oxidized_cut_copper_slab": "waxed_oxidized_cut_copper",
    "blackstone_slab": "blackstone",
    "polished_blackstone_slab": "polished_blackstone_top",
    "polished_blackstone_brick_slab": "polished_blackstone_bricks",
    "petrified_oak_slab": "oak_planks",
    "mud_brick_slab": "mud_bricks",
    "deepslate_brick_slab": "deepslate_bricks",
    "deepslate_tile_slab": "deepslate_tiles",
    "cobbled_deepslate_slab": "cobbled_deepslate",
    "polished_deepslate_slab": "polished_deepslate"
}

base_blockstates = 'src/main/resources/assets/blockparts/blockstates'
base_models_block = 'src/main/resources/assets/blockparts/models/block'
base_models_item = 'src/main/resources/assets/blockparts/models/item'

os.makedirs(base_blockstates, exist_ok=True)
os.makedirs(base_models_block, exist_ok=True)
os.makedirs(base_models_item, exist_ok=True)

for slab, texture in SLABS.items():
    material = slab[:-5] if slab.endswith('_slab') else slab
    for part in PARTS.keys():
        block_name = f"{material}_{part}"

        state = {
            "variants": {
                "axis=x": {"model": f"blockparts:block/{block_name}_x"},
                "axis=y": {"model": f"blockparts:block/{block_name}_y"},
                "axis=z": {"model": f"blockparts:block/{block_name}_z"}
            }
        }
        with open(os.path.join(base_blockstates, f"{block_name}.json"), 'w') as f:
            json.dump(state, f, indent=2)

        for axis in ['x', 'y', 'z']:
            model = {
                "parent": f"blockparts:block/{part}_{axis}",
                "textures": {
                    "particle": f"minecraft:block/{texture}",
                    "down": f"minecraft:block/{texture}",
                    "up": f"minecraft:block/{texture}",
                    "north": f"minecraft:block/{texture}",
                    "south": f"minecraft:block/{texture}",
                    "west": f"minecraft:block/{texture}",
                    "east": f"minecraft:block/{texture}"
                }
            }
            with open(os.path.join(base_models_block, f"{block_name}_{axis}.json"), 'w') as f:
                json.dump(model, f, indent=2)

        item_model = {"parent": f"blockparts:block/{block_name}_y"}
        with open(os.path.join(base_models_item, f"{block_name}.json"), 'w') as f:
            json.dump(item_model, f, indent=2)

print("Generated part JSONs for", len(SLABS) * len(PARTS), "blocks")
