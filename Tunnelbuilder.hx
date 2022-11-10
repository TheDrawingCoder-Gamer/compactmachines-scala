import sys.io.File;

function genJson(indicator : String, wall: String) {
    return '{ "parent": "compactmachines:tunnels/base_toggleable", "textures": { "wall": "compactmachines:block/tunnels/${wall}", "indicatortile": "compactmachines:block/tunnels/indicator_tile_$indicator" } }';
}

function main() {
final walls = ["north", "south", "east", "west", "up", "down", "none"];
final indicatortile = ["incoming", "neither", "outgoing"];
var variants : Dynamic = {};
for (wall in walls) {
  for (tile in indicatortile) {
    File.saveContent('src/main/generated/assets/compactmachines/models/block/tunnels/${wall}_${tile}.json', genJson(tile, wall));
    Reflect.setField(variants, 'connected_side=$wall,going=$tile', { "model": 'compactmachines:block/tunnels/${wall}_${tile}' });
  }
}

File.saveContent("src/main/generated/assets/compactmachines/blockstate/tunnel_wall.json", haxe.Json.stringify({"variants": variants}));
}
