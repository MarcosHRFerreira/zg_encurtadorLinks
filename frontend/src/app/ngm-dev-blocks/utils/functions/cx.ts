/*
	Installed from https://ui.angular-material.dev/api/registry/
	Update this file using `@ngm-dev/cli update utils/functions`
*/

import { twMerge } from 'tailwind-merge';
import clsx from 'clsx';

export function cx(...args: any[]): string {
  return twMerge(clsx(...args));
}
