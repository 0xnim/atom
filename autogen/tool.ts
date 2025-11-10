// generate_tools.ts
import {stringify} from "yaml";


const MATERIALS = ["iron", "copper", "steel"] as const;
type Material = (typeof MATERIALS)[number];
const ALL_TOOLS = [
    "pickaxe",
    "shovel",
    "hoe",
    "sword",
    "axe",
    "hammer",
    "knife",
    "saw"
] as const;
type ToolType = (typeof ALL_TOOLS)[number];
const CLASSIC_TOOLS: ToolType[] = [
    "pickaxe",
    "shovel",
    "hoe",
    "sword",
    "axe",
];

function shouldGenerateFullTool(material: Material, type: ToolType): boolean {
    if (material === "steel") return true; // all full tools for steel
    const isClassic = CLASSIC_TOOLS.includes(type);
    return !isClassic;
}

// Texture paths (replace with your assets)
function fullToolTexture(material: Material, type: ToolType): string {
    // Example: minecraft:item/steel_pickaxe, minecraft:item/iron_hammer, etc.
    return `minecraft:item/tool/${material}_${type}`;
}

function headTexture(material: Material, type: ToolType): string {
    // Example: minecraft:item/steel_pickaxe_head, minecraft:item/iron_hammer_head
    return `minecraft:item/tool/head/${material}_${type}`;
}

// Base item materials for full tools
function fullToolBaseMaterial(type: ToolType): string {
    // Use closest vanilla bases for behavior; customize if you have custom items
    switch (type) {
        case "pickaxe":
            return "iron_pickaxe";
        case "shovel":
            return "iron_shovel";
        case "hoe":
            return "iron_hoe";
        case "sword":
            return "iron_sword";
        case "axe":
            return "iron_axe";
        // For new tools, map to something neutral; replace with custom if available
        case "hammer":
            return "iron_pickaxe";
        case "knife":
            return "iron_sword";
        case "saw":
            return "iron_axe";
    }
}

// Base item for heads (inert)
const HEAD_BASE_MATERIAL = "paper";

// Keys
function headKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}_head`;
}

function toolKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}`;
}

// L10n refs
function headL10n(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool_head.${material}.${type}.name>`;
}

function toolL10n(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool.${material}.${type}.name>`;
}

function headItemBlock(material: Material, type: ToolType) {
    return {
        [headKey(material, type)]: {
            material: HEAD_BASE_MATERIAL,
            data: {
                "item-name": headL10n(material, type),
                lore: [
                    "<!i><gray><lang:item.tool_head.common.lore>",
                    "",
                    "<!i><white><image:atom:badge_material> <image:atom:badge_age_copper>",
                ],
                "remove-components": ["attribute_modifiers"],
            },
            model: {
                template: "default:model/simplified_generated",
                arguments: {
                    path: headTexture(material, type),
                },
            },
        },
    };
}

function fullToolItemBlock(material: Material, type: ToolType) {
    return {
        [toolKey(material, type)]: {
            material: fullToolBaseMaterial(type),
            data: {
                "item-name": toolL10n(material, type),
                lore: [
                    "<!i><gray><lang:item.tool.common.lore>",
                    "",
                    "<!i><white><image:atom:badge_tool> <image:atom:badge_age_copper>",
                ],
                "remove-components": ["attribute_modifiers"],
            },
            model: {
                template: "default:model/simplified_generated",
                arguments: {
                    path: fullToolTexture(material, type),
                },
            },
        },
    };
}

function generateDoc() {
    const items: Record<string, unknown> = {};
    const headKeys: string[] = [];
    const toolKeys: string[] = [];

    for (const mat of MATERIALS) {
        for (const t of ALL_TOOLS) {
            // Always generate heads
            Object.assign(items, headItemBlock(mat, t));
            headKeys.push(headKey(mat, t));

            if (shouldGenerateFullTool(mat, t)) {
                Object.assign(items, fullToolItemBlock(mat, t));
                toolKeys.push(toolKey(mat, t));
            }
        }
    }

    const categories = {
        "atom:tools": {
            name: "<!i><white><lang:category.tools.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.tools.lore>"],
            icon: "minecraft:copper_pickaxe", // adjust
            list: toolKeys,
        },
        "atom:tool_heads": {
            name: "<!i><white><lang:category.tool_heads.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.tool_heads.lore>"],
            icon: "atom:copper_pickaxe_head", // adjust
            list: headKeys,
        },
    };

    // Translations
    const en: Record<string, string> = {
        "category.tools.name": "Tools",
        "category.tools.lore": "Crafted tools by material",
        "category.tool_heads.name": "Tool Heads",
        "category.tool_heads.lore": "Components used to craft tools",
        "item.tool_head.common.lore": "A shaped head for a tool",
        "item.tool.common.lore": "A durable tool for survival tasks",
    };

    const materialName: Record<Material, string> = {
        iron: "Iron",
        copper: "Copper",
        steel: "Steel",
    };
    const typeName: Record<ToolType, string> = {
        pickaxe: "Pickaxe",
        shovel: "Shovel",
        hoe: "Hoe",
        sword: "Sword",
        axe: "Axe",
        hammer: "Hammer",
        knife: "Knife",
        saw: "Saw",
    };

    for (const m of MATERIALS) {
        for (const t of ALL_TOOLS) {
            en[`item.tool_head.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]} Head`;
            en[`item.tool.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]}`;
        }
    }

    return {items, categories, lang: {en}};
}

const yaml = stringify(generateDoc(), {lineWidth: 0});
await Bun.write("../run/plugins/CraftEngine/resources/atom/configuration/auto/tools.yml", yaml);
console.log("Generated tools.yml (heads for all; full tools for steel + all new tools)");