# -*- coding: utf-8 -*
import argparse
import numpy as np
import math


def dim2loc(args):
    shapes = np.array(args.shape.split(',')).astype(np.int32)  # 维度
    offsets = np.array(args.offset.split(',')).astype(np.int32)  # 偏移
    offset = 0
    for i in range(len(shapes)):
        bucket = 1  # 剩余维度容量
        for j in range(i + 1, len(shapes)):
            bucket *= shapes[j]
        offset += offsets[i] * bucket
    print(offset)


def loc2dim(args):
    offset = int(args.offset)  # 偏移
    shapes = np.array(args.shape.split(',')).astype(np.int32)  # 维度
    offsets = []
    print('i', 'offset', 'quotient', 'remainder')
    for i in range(len(shapes)):
        bucket = 1  # 剩余维度容量
        for j in range(i + 1, len(shapes)):
            bucket *= shapes[j]
        quotient = int(math.floor(offset / bucket))  # 商
        remainder = offset % bucket  # 余
        print(i, offset, quotient, remainder)
        offset = remainder
        offsets.append(quotient)
    print(offsets)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("command", help="'dim2loc' or 'loc2dim'")
    parser.add_argument('--shape', required=True, help='matrix shape')
    parser.add_argument('--offset', required=True, help='matrix offset')
    args = parser.parse_args()

    ({'dim2loc': dim2loc, 'loc2dim': loc2dim})[args.command](args)
