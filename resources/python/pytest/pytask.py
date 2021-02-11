import sys 
import os
from argparse import ArgumentParser
print(str("Python Version: " + sys.version[:5]).center(50,'-'))

_project_name = "TS-CHIEF-DEV"
_root = os.path.abspath(".")[0:os.path.abspath(".").find(_project_name) + len(_project_name)]

parser = ArgumentParser()
parser.add_argument("--project_root", "--root", help="project root", type=str, default=_root)
parser.add_argument("--working_dir", "--wdir", help="working directory", type=str, default=".")
parser.add_argument("--dry_run", "--dry", help="dry run commands", action="store_true", default=False)

parser.add_argument("--generate_kernels", help="generate kernels", nargs='*', default=[])


args, unknown_args = parser.parse_known_args()
print("known: "  + str(args))
print("unknown: " + str(unknown_args))

_root = os.path.abspath(args.project_root) if args.project_root is not None else _root
sys.path.extend([_root])
print('root: ' + _root)
args.working_dir = os.path.abspath(args.working_dir) if args.working_dir is not None else '.'
os.chdir(args.working_dir)
print("working_dir: " + args.working_dir)



import time
# time.sleep( 5 )

if args.generate_kernels:
    print('generating kernels')
    
    from rocket_tree.rocket_functions import generate_kernels
    
    kernels = generate_kernels(10,10)