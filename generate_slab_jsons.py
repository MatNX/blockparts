slabs = {
    # wood slabs
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
    # stone slabs
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
    "polished_deepslate_slab": "polished_deepslate",
}

import os, json

base_blockstates = 'src/main/resources/assets/minecraft/blockstates'
base_models = 'src/main/resources/assets/blockparts/models/block'
os.makedirs(base_blockstates, exist_ok=True)
os.makedirs(base_models, exist_ok=True)

for slab, texture in slabs.items():
    # blockstate
    blockstate = {
        "variants": {
            "axis=x": {"model": f"blockparts:block/{slab}_x"},
            "axis=y": {"model": f"blockparts:block/{slab}_y"},
            "axis=z": {"model": f"blockparts:block/{slab}_z"}
        }
    }
    with open(os.path.join(base_blockstates, f"{slab}.json"), 'w') as f:
        json.dump(blockstate, f, indent=2)

    for axis in ['x','y','z']:
        model = {
            "parent": f"blockparts:block/slab_{axis}",
            "textures": {
                "bottom": f"minecraft:block/{texture}",
                "side": f"minecraft:block/{texture}",
                "top": f"minecraft:block/{texture}"
            }
        }
        with open(os.path.join(base_models, f"{slab}_{axis}.json"), 'w') as f:
            json.dump(model, f, indent=2)
