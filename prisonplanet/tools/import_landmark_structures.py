"""Import authored structure templates and attach Condemned loot references.

This keeps the creative-world source templates untouched. The prepared copies
are written beneath the mod data namespace for packaging into the mod jar.
"""

from __future__ import annotations

import argparse
import gzip
import pathlib
import struct
from typing import Any


TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

Tag = tuple[int, Any]
Compound = dict[str, Tag]


class Reader:
    def __init__(self, data: bytes) -> None:
        self.data = data
        self.index = 0

    def take(self, size: int) -> bytes:
        result = self.data[self.index:self.index + size]
        self.index += size
        return result

    def unpack(self, fmt: str) -> Any:
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, self.take(size))[0]

    def string(self) -> str:
        return self.take(self.unpack(">H")).decode("utf-8")

    def payload(self, tag_type: int) -> Any:
        if tag_type == TAG_END:
            return None
        if tag_type == TAG_BYTE:
            return self.unpack(">b")
        if tag_type == TAG_SHORT:
            return self.unpack(">h")
        if tag_type == TAG_INT:
            return self.unpack(">i")
        if tag_type == TAG_LONG:
            return self.unpack(">q")
        if tag_type == TAG_FLOAT:
            return self.unpack(">f")
        if tag_type == TAG_DOUBLE:
            return self.unpack(">d")
        if tag_type == TAG_BYTE_ARRAY:
            return self.take(self.unpack(">i"))
        if tag_type == TAG_STRING:
            return self.string()
        if tag_type == TAG_LIST:
            child_type = self.unpack(">b")
            return child_type, [self.payload(child_type) for _ in range(self.unpack(">i"))]
        if tag_type == TAG_COMPOUND:
            result: Compound = {}
            while True:
                child_type = self.unpack(">b")
                if child_type == TAG_END:
                    return result
                child_name = self.string()
                result[child_name] = child_type, self.payload(child_type)
        if tag_type == TAG_INT_ARRAY:
            return [self.unpack(">i") for _ in range(self.unpack(">i"))]
        if tag_type == TAG_LONG_ARRAY:
            return [self.unpack(">q") for _ in range(self.unpack(">i"))]
        raise ValueError(f"Unsupported NBT tag type {tag_type}")


def pack_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def pack_payload(tag_type: int, value: Any) -> bytes:
    if tag_type == TAG_END:
        return b""
    if tag_type == TAG_BYTE:
        return struct.pack(">b", value)
    if tag_type == TAG_SHORT:
        return struct.pack(">h", value)
    if tag_type == TAG_INT:
        return struct.pack(">i", value)
    if tag_type == TAG_LONG:
        return struct.pack(">q", value)
    if tag_type == TAG_FLOAT:
        return struct.pack(">f", value)
    if tag_type == TAG_DOUBLE:
        return struct.pack(">d", value)
    if tag_type == TAG_BYTE_ARRAY:
        return struct.pack(">i", len(value)) + value
    if tag_type == TAG_STRING:
        return pack_string(value)
    if tag_type == TAG_LIST:
        child_type, children = value
        return (
            struct.pack(">bi", child_type, len(children))
            + b"".join(pack_payload(child_type, child) for child in children)
        )
    if tag_type == TAG_COMPOUND:
        content = b"".join(
            struct.pack(">b", child_type) + pack_string(name) + pack_payload(child_type, child_value)
            for name, (child_type, child_value) in value.items()
        )
        return content + b"\0"
    if tag_type == TAG_INT_ARRAY:
        return struct.pack(">i", len(value)) + b"".join(struct.pack(">i", item) for item in value)
    if tag_type == TAG_LONG_ARRAY:
        return struct.pack(">i", len(value)) + b"".join(struct.pack(">q", item) for item in value)
    raise ValueError(f"Unsupported NBT tag type {tag_type}")


def read_root(path: pathlib.Path) -> tuple[str, Compound]:
    raw = gzip.decompress(path.read_bytes())
    reader = Reader(raw)
    if reader.unpack(">b") != TAG_COMPOUND:
        raise ValueError(f"{path} does not contain a compound root tag")
    return reader.string(), reader.payload(TAG_COMPOUND)


def write_root(path: pathlib.Path, name: str, compound: Compound) -> None:
    raw = struct.pack(">b", TAG_COMPOUND) + pack_string(name) + pack_payload(TAG_COMPOUND, compound)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(raw, compresslevel=9, mtime=0))


def list_values(compound: Compound, name: str) -> list[Any]:
    _, (_, values) = compound[name]
    return values


def text(compound: Compound, name: str) -> str:
    return compound[name][1]


def attach_table(nbt: Compound, table: str) -> None:
    nbt.pop("Items", None)
    nbt["LootTable"] = TAG_STRING, table


def configure_structure(file_name: str, root: Compound) -> tuple[int, int]:
    palette = list_values(root, "palette")
    palette_names = [text(state, "Name") for state in palette]
    containers = 0
    spawners = 0
    for block in list_values(root, "blocks"):
        block_name = palette_names[block["state"][1]]
        nbt = block.get("nbt", (TAG_COMPOUND, {}))[1]
        if file_name == "safe_house.nbt" and block_name in {"minecraft:barrel", "minecraft:chest"}:
            attach_table(nbt, "prisonplanet:chests/safe_house_supplies")
            block["nbt"] = TAG_COMPOUND, nbt
            containers += 1
        if file_name == "rustwarden_keep.nbt" and block_name == "minecraft:trial_spawner":
            reward = {
                "data": (TAG_STRING, "prisonplanet:chests/rustwarden_trial_reward"),
                "weight": (TAG_INT, 1),
            }
            normal_config = nbt.get("normal_config", (TAG_COMPOUND, {}))[1]
            normal_config["loot_tables_to_eject"] = TAG_LIST, (TAG_COMPOUND, [reward])
            nbt["normal_config"] = TAG_COMPOUND, normal_config
            block["nbt"] = TAG_COMPOUND, nbt
            spawners += 1
    return containers, spawners


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()

    total_containers = 0
    total_spawners = 0
    for source_path in sorted(args.source.glob("*.nbt")):
        root_name, root = read_root(source_path)
        containers, spawners = configure_structure(source_path.name, root)
        write_root(args.output / source_path.name, root_name, root)
        # Expose friendly root IDs for /place structure testing as well as pool use.
        write_root(args.output.parent / source_path.name, root_name, root)
        total_containers += containers
        total_spawners += spawners
        print(f"Imported {source_path.name}: containers={containers}, trial_spawners={spawners}")
    print(f"Configured totals: containers={total_containers}, trial_spawners={total_spawners}")


if __name__ == "__main__":
    main()
