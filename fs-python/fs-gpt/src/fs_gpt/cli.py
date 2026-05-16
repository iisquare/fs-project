import sys
from enum import Enum, unique
from pathlib import Path

import yaml

from .env import VERSION

USAGE = (
    "Usage:\n"
    + "  version: show version info\n"
)

WELCOME = (
    f"Welcome to FS Project, version {VERSION}\n"
)


@unique
class Command(str, Enum):
    VERSION = "version"
    HELP = "help"
    RUN = "run"


def run():
    if len(sys.argv) != 2 or not (sys.argv[1].endswith(".yaml") or sys.argv[1].endswith(".yml")):
        raise NotImplementedError("Run command like 'fs-gpt run /path/to/config.yaml'")
    args = yaml.safe_load(Path(sys.argv[1]).read_text())
    for name in ['model_name_or_path']:  # 绝对化路径
        if name in args:
            args[name] = str(Path(args[name]).absolute())
    match args['action']:
        case 'api':
            from fs_gpt.api import server
            server.main(args)
        case 'derive':
            from fs_gpt.derive import derive
            derive.main(args)
        case 'train':
            from fs_gpt.train import tuner
            tuner.main(args)
        case 'vocab':
            from fs_gpt.vocab import vocab
            vocab.main(args)
        case "bert":
            from fs_gpt.bert import bert
            bert.main(args)
        case _:
            print(f"Unknown action: {args['action']}")


def main():
    command = sys.argv.pop(1) if len(sys.argv) != 1 else Command.HELP
    if command == Command.VERSION:
        print(WELCOME)
    elif command == Command.HELP:
        print(USAGE)
    elif command == Command.RUN:
        run()
    else:
        raise NotImplementedError(f"Unknown command: {command}.")


if __name__ == "__main__":
    main()
