/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.apache.xerces.util

import java.util.Arrays


object XMLChar {

  /**
   Character flags.
   */
  private val CHARS = new Array[Byte](1 << 16)

  /**
   Valid character mask.
   */
  val MASK_VALID = 0x01

  /**
   Space character mask.
   */
  val MASK_SPACE = 0x02

  /**
   Name start character mask.
   */
  val MASK_NAME_START = 0x04

  /**
   Name character mask.
   */
  val MASK_NAME = 0x08

  /**
   Pubid character mask.
   */
  val MASK_PUBID = 0x10

  /**
   * Content character mask. Special characters are those that can
   * be considered the start of markup, such as '<' and '&'.
   * The various newline characters are considered special as well.
   * All other valid XML characters can be considered content.
   *
   * This is an optimization for the inner loop of character scanning.
   */
  val MASK_CONTENT = 0x20

  /**
   NCName start character mask.
   */
  val MASK_NCNAME_START = 0x40

  /**
   NCName character mask.
   */
  val MASK_NCNAME = 0x80

  CHARS(9) = 35

  CHARS(10) = 19

  CHARS(13) = 19

  CHARS(32) = 51

  CHARS(33) = 49

  CHARS(34) = 33

  Arrays.fill(CHARS, 35, 38, 49.toByte)

  CHARS(38) = 1

  Arrays.fill(CHARS, 39, 45, 49.toByte)

  Arrays.fill(CHARS, 45, 47, -71.toByte)

  CHARS(47) = 49

  Arrays.fill(CHARS, 48, 58, -71.toByte)

  CHARS(58) = 61

  CHARS(59) = 49

  CHARS(60) = 1

  CHARS(61) = 49

  CHARS(62) = 33

  Arrays.fill(CHARS, 63, 65, 49.toByte)

  Arrays.fill(CHARS, 65, 91, -3.toByte)

  Arrays.fill(CHARS, 91, 93, 33.toByte)

  CHARS(93) = 1

  CHARS(94) = 33

  CHARS(95) = -3

  CHARS(96) = 33

  Arrays.fill(CHARS, 97, 123, -3.toByte)

  Arrays.fill(CHARS, 123, 183, 33.toByte)

  CHARS(183) = -87

  Arrays.fill(CHARS, 184, 192, 33.toByte)

  Arrays.fill(CHARS, 192, 215, -19.toByte)

  CHARS(215) = 33

  Arrays.fill(CHARS, 216, 247, -19.toByte)

  CHARS(247) = 33

  Arrays.fill(CHARS, 248, 306, -19.toByte)

  Arrays.fill(CHARS, 306, 308, 33.toByte)

  Arrays.fill(CHARS, 308, 319, -19.toByte)

  Arrays.fill(CHARS, 319, 321, 33.toByte)

  Arrays.fill(CHARS, 321, 329, -19.toByte)

  CHARS(329) = 33

  Arrays.fill(CHARS, 330, 383, -19.toByte)

  CHARS(383) = 33

  Arrays.fill(CHARS, 384, 452, -19.toByte)

  Arrays.fill(CHARS, 452, 461, 33.toByte)

  Arrays.fill(CHARS, 461, 497, -19.toByte)

  Arrays.fill(CHARS, 497, 500, 33.toByte)

  Arrays.fill(CHARS, 500, 502, -19.toByte)

  Arrays.fill(CHARS, 502, 506, 33.toByte)

  Arrays.fill(CHARS, 506, 536, -19.toByte)

  Arrays.fill(CHARS, 536, 592, 33.toByte)

  Arrays.fill(CHARS, 592, 681, -19.toByte)

  Arrays.fill(CHARS, 681, 699, 33.toByte)

  Arrays.fill(CHARS, 699, 706, -19.toByte)

  Arrays.fill(CHARS, 706, 720, 33.toByte)

  Arrays.fill(CHARS, 720, 722, -87.toByte)

  Arrays.fill(CHARS, 722, 768, 33.toByte)

  Arrays.fill(CHARS, 768, 838, -87.toByte)

  Arrays.fill(CHARS, 838, 864, 33.toByte)

  Arrays.fill(CHARS, 864, 866, -87.toByte)

  Arrays.fill(CHARS, 866, 902, 33.toByte)

  CHARS(902) = -19

  CHARS(903) = -87

  Arrays.fill(CHARS, 904, 907, -19.toByte)

  CHARS(907) = 33

  CHARS(908) = -19

  CHARS(909) = 33

  Arrays.fill(CHARS, 910, 930, -19.toByte)

  CHARS(930) = 33

  Arrays.fill(CHARS, 931, 975, -19.toByte)

  CHARS(975) = 33

  Arrays.fill(CHARS, 976, 983, -19.toByte)

  Arrays.fill(CHARS, 983, 986, 33.toByte)

  CHARS(986) = -19

  CHARS(987) = 33

  CHARS(988) = -19

  CHARS(989) = 33

  CHARS(990) = -19

  CHARS(991) = 33

  CHARS(992) = -19

  CHARS(993) = 33

  Arrays.fill(CHARS, 994, 1012, -19.toByte)

  Arrays.fill(CHARS, 1012, 1025, 33.toByte)

  Arrays.fill(CHARS, 1025, 1037, -19.toByte)

  CHARS(1037) = 33

  Arrays.fill(CHARS, 1038, 1104, -19.toByte)

  CHARS(1104) = 33

  Arrays.fill(CHARS, 1105, 1117, -19.toByte)

  CHARS(1117) = 33

  Arrays.fill(CHARS, 1118, 1154, -19.toByte)

  CHARS(1154) = 33

  Arrays.fill(CHARS, 1155, 1159, -87.toByte)

  Arrays.fill(CHARS, 1159, 1168, 33.toByte)

  Arrays.fill(CHARS, 1168, 1221, -19.toByte)

  Arrays.fill(CHARS, 1221, 1223, 33.toByte)

  Arrays.fill(CHARS, 1223, 1225, -19.toByte)

  Arrays.fill(CHARS, 1225, 1227, 33.toByte)

  Arrays.fill(CHARS, 1227, 1229, -19.toByte)

  Arrays.fill(CHARS, 1229, 1232, 33.toByte)

  Arrays.fill(CHARS, 1232, 1260, -19.toByte)

  Arrays.fill(CHARS, 1260, 1262, 33.toByte)

  Arrays.fill(CHARS, 1262, 1270, -19.toByte)

  Arrays.fill(CHARS, 1270, 1272, 33.toByte)

  Arrays.fill(CHARS, 1272, 1274, -19.toByte)

  Arrays.fill(CHARS, 1274, 1329, 33.toByte)

  Arrays.fill(CHARS, 1329, 1367, -19.toByte)

  Arrays.fill(CHARS, 1367, 1369, 33.toByte)

  CHARS(1369) = -19

  Arrays.fill(CHARS, 1370, 1377, 33.toByte)

  Arrays.fill(CHARS, 1377, 1415, -19.toByte)

  Arrays.fill(CHARS, 1415, 1425, 33.toByte)

  Arrays.fill(CHARS, 1425, 1442, -87.toByte)

  CHARS(1442) = 33

  Arrays.fill(CHARS, 1443, 1466, -87.toByte)

  CHARS(1466) = 33

  Arrays.fill(CHARS, 1467, 1470, -87.toByte)

  CHARS(1470) = 33

  CHARS(1471) = -87

  CHARS(1472) = 33

  Arrays.fill(CHARS, 1473, 1475, -87.toByte)

  CHARS(1475) = 33

  CHARS(1476) = -87

  Arrays.fill(CHARS, 1477, 1488, 33.toByte)

  Arrays.fill(CHARS, 1488, 1515, -19.toByte)

  Arrays.fill(CHARS, 1515, 1520, 33.toByte)

  Arrays.fill(CHARS, 1520, 1523, -19.toByte)

  Arrays.fill(CHARS, 1523, 1569, 33.toByte)

  Arrays.fill(CHARS, 1569, 1595, -19.toByte)

  Arrays.fill(CHARS, 1595, 1600, 33.toByte)

  CHARS(1600) = -87

  Arrays.fill(CHARS, 1601, 1611, -19.toByte)

  Arrays.fill(CHARS, 1611, 1619, -87.toByte)

  Arrays.fill(CHARS, 1619, 1632, 33.toByte)

  Arrays.fill(CHARS, 1632, 1642, -87.toByte)

  Arrays.fill(CHARS, 1642, 1648, 33.toByte)

  CHARS(1648) = -87

  Arrays.fill(CHARS, 1649, 1720, -19.toByte)

  Arrays.fill(CHARS, 1720, 1722, 33.toByte)

  Arrays.fill(CHARS, 1722, 1727, -19.toByte)

  CHARS(1727) = 33

  Arrays.fill(CHARS, 1728, 1743, -19.toByte)

  CHARS(1743) = 33

  Arrays.fill(CHARS, 1744, 1748, -19.toByte)

  CHARS(1748) = 33

  CHARS(1749) = -19

  Arrays.fill(CHARS, 1750, 1765, -87.toByte)

  Arrays.fill(CHARS, 1765, 1767, -19.toByte)

  Arrays.fill(CHARS, 1767, 1769, -87.toByte)

  CHARS(1769) = 33

  Arrays.fill(CHARS, 1770, 1774, -87.toByte)

  Arrays.fill(CHARS, 1774, 1776, 33.toByte)

  Arrays.fill(CHARS, 1776, 1786, -87.toByte)

  Arrays.fill(CHARS, 1786, 2305, 33.toByte)

  Arrays.fill(CHARS, 2305, 2308, -87.toByte)

  CHARS(2308) = 33

  Arrays.fill(CHARS, 2309, 2362, -19.toByte)

  Arrays.fill(CHARS, 2362, 2364, 33.toByte)

  CHARS(2364) = -87

  CHARS(2365) = -19

  Arrays.fill(CHARS, 2366, 2382, -87.toByte)

  Arrays.fill(CHARS, 2382, 2385, 33.toByte)

  Arrays.fill(CHARS, 2385, 2389, -87.toByte)

  Arrays.fill(CHARS, 2389, 2392, 33.toByte)

  Arrays.fill(CHARS, 2392, 2402, -19.toByte)

  Arrays.fill(CHARS, 2402, 2404, -87.toByte)

  Arrays.fill(CHARS, 2404, 2406, 33.toByte)

  Arrays.fill(CHARS, 2406, 2416, -87.toByte)

  Arrays.fill(CHARS, 2416, 2433, 33.toByte)

  Arrays.fill(CHARS, 2433, 2436, -87.toByte)

  CHARS(2436) = 33

  Arrays.fill(CHARS, 2437, 2445, -19.toByte)

  Arrays.fill(CHARS, 2445, 2447, 33.toByte)

  Arrays.fill(CHARS, 2447, 2449, -19.toByte)

  Arrays.fill(CHARS, 2449, 2451, 33.toByte)

  Arrays.fill(CHARS, 2451, 2473, -19.toByte)

  CHARS(2473) = 33

  Arrays.fill(CHARS, 2474, 2481, -19.toByte)

  CHARS(2481) = 33

  CHARS(2482) = -19

  Arrays.fill(CHARS, 2483, 2486, 33.toByte)

  Arrays.fill(CHARS, 2486, 2490, -19.toByte)

  Arrays.fill(CHARS, 2490, 2492, 33.toByte)

  CHARS(2492) = -87

  CHARS(2493) = 33

  Arrays.fill(CHARS, 2494, 2501, -87.toByte)

  Arrays.fill(CHARS, 2501, 2503, 33.toByte)

  Arrays.fill(CHARS, 2503, 2505, -87.toByte)

  Arrays.fill(CHARS, 2505, 2507, 33.toByte)

  Arrays.fill(CHARS, 2507, 2510, -87.toByte)

  Arrays.fill(CHARS, 2510, 2519, 33.toByte)

  CHARS(2519) = -87

  Arrays.fill(CHARS, 2520, 2524, 33.toByte)

  Arrays.fill(CHARS, 2524, 2526, -19.toByte)

  CHARS(2526) = 33

  Arrays.fill(CHARS, 2527, 2530, -19.toByte)

  Arrays.fill(CHARS, 2530, 2532, -87.toByte)

  Arrays.fill(CHARS, 2532, 2534, 33.toByte)

  Arrays.fill(CHARS, 2534, 2544, -87.toByte)

  Arrays.fill(CHARS, 2544, 2546, -19.toByte)

  Arrays.fill(CHARS, 2546, 2562, 33.toByte)

  CHARS(2562) = -87

  Arrays.fill(CHARS, 2563, 2565, 33.toByte)

  Arrays.fill(CHARS, 2565, 2571, -19.toByte)

  Arrays.fill(CHARS, 2571, 2575, 33.toByte)

  Arrays.fill(CHARS, 2575, 2577, -19.toByte)

  Arrays.fill(CHARS, 2577, 2579, 33.toByte)

  Arrays.fill(CHARS, 2579, 2601, -19.toByte)

  CHARS(2601) = 33

  Arrays.fill(CHARS, 2602, 2609, -19.toByte)

  CHARS(2609) = 33

  Arrays.fill(CHARS, 2610, 2612, -19.toByte)

  CHARS(2612) = 33

  Arrays.fill(CHARS, 2613, 2615, -19.toByte)

  CHARS(2615) = 33

  Arrays.fill(CHARS, 2616, 2618, -19.toByte)

  Arrays.fill(CHARS, 2618, 2620, 33.toByte)

  CHARS(2620) = -87

  CHARS(2621) = 33

  Arrays.fill(CHARS, 2622, 2627, -87.toByte)

  Arrays.fill(CHARS, 2627, 2631, 33.toByte)

  Arrays.fill(CHARS, 2631, 2633, -87.toByte)

  Arrays.fill(CHARS, 2633, 2635, 33.toByte)

  Arrays.fill(CHARS, 2635, 2638, -87.toByte)

  Arrays.fill(CHARS, 2638, 2649, 33.toByte)

  Arrays.fill(CHARS, 2649, 2653, -19.toByte)

  CHARS(2653) = 33

  CHARS(2654) = -19

  Arrays.fill(CHARS, 2655, 2662, 33.toByte)

  Arrays.fill(CHARS, 2662, 2674, -87.toByte)

  Arrays.fill(CHARS, 2674, 2677, -19.toByte)

  Arrays.fill(CHARS, 2677, 2689, 33.toByte)

  Arrays.fill(CHARS, 2689, 2692, -87.toByte)

  CHARS(2692) = 33

  Arrays.fill(CHARS, 2693, 2700, -19.toByte)

  CHARS(2700) = 33

  CHARS(2701) = -19

  CHARS(2702) = 33

  Arrays.fill(CHARS, 2703, 2706, -19.toByte)

  CHARS(2706) = 33

  Arrays.fill(CHARS, 2707, 2729, -19.toByte)

  CHARS(2729) = 33

  Arrays.fill(CHARS, 2730, 2737, -19.toByte)

  CHARS(2737) = 33

  Arrays.fill(CHARS, 2738, 2740, -19.toByte)

  CHARS(2740) = 33

  Arrays.fill(CHARS, 2741, 2746, -19.toByte)

  Arrays.fill(CHARS, 2746, 2748, 33.toByte)

  CHARS(2748) = -87

  CHARS(2749) = -19

  Arrays.fill(CHARS, 2750, 2758, -87.toByte)

  CHARS(2758) = 33

  Arrays.fill(CHARS, 2759, 2762, -87.toByte)

  CHARS(2762) = 33

  Arrays.fill(CHARS, 2763, 2766, -87.toByte)

  Arrays.fill(CHARS, 2766, 2784, 33.toByte)

  CHARS(2784) = -19

  Arrays.fill(CHARS, 2785, 2790, 33.toByte)

  Arrays.fill(CHARS, 2790, 2800, -87.toByte)

  Arrays.fill(CHARS, 2800, 2817, 33.toByte)

  Arrays.fill(CHARS, 2817, 2820, -87.toByte)

  CHARS(2820) = 33

  Arrays.fill(CHARS, 2821, 2829, -19.toByte)

  Arrays.fill(CHARS, 2829, 2831, 33.toByte)

  Arrays.fill(CHARS, 2831, 2833, -19.toByte)

  Arrays.fill(CHARS, 2833, 2835, 33.toByte)

  Arrays.fill(CHARS, 2835, 2857, -19.toByte)

  CHARS(2857) = 33

  Arrays.fill(CHARS, 2858, 2865, -19.toByte)

  CHARS(2865) = 33

  Arrays.fill(CHARS, 2866, 2868, -19.toByte)

  Arrays.fill(CHARS, 2868, 2870, 33.toByte)

  Arrays.fill(CHARS, 2870, 2874, -19.toByte)

  Arrays.fill(CHARS, 2874, 2876, 33.toByte)

  CHARS(2876) = -87

  CHARS(2877) = -19

  Arrays.fill(CHARS, 2878, 2884, -87.toByte)

  Arrays.fill(CHARS, 2884, 2887, 33.toByte)

  Arrays.fill(CHARS, 2887, 2889, -87.toByte)

  Arrays.fill(CHARS, 2889, 2891, 33.toByte)

  Arrays.fill(CHARS, 2891, 2894, -87.toByte)

  Arrays.fill(CHARS, 2894, 2902, 33.toByte)

  Arrays.fill(CHARS, 2902, 2904, -87.toByte)

  Arrays.fill(CHARS, 2904, 2908, 33.toByte)

  Arrays.fill(CHARS, 2908, 2910, -19.toByte)

  CHARS(2910) = 33

  Arrays.fill(CHARS, 2911, 2914, -19.toByte)

  Arrays.fill(CHARS, 2914, 2918, 33.toByte)

  Arrays.fill(CHARS, 2918, 2928, -87.toByte)

  Arrays.fill(CHARS, 2928, 2946, 33.toByte)

  Arrays.fill(CHARS, 2946, 2948, -87.toByte)

  CHARS(2948) = 33

  Arrays.fill(CHARS, 2949, 2955, -19.toByte)

  Arrays.fill(CHARS, 2955, 2958, 33.toByte)

  Arrays.fill(CHARS, 2958, 2961, -19.toByte)

  CHARS(2961) = 33

  Arrays.fill(CHARS, 2962, 2966, -19.toByte)

  Arrays.fill(CHARS, 2966, 2969, 33.toByte)

  Arrays.fill(CHARS, 2969, 2971, -19.toByte)

  CHARS(2971) = 33

  CHARS(2972) = -19

  CHARS(2973) = 33

  Arrays.fill(CHARS, 2974, 2976, -19.toByte)

  Arrays.fill(CHARS, 2976, 2979, 33.toByte)

  Arrays.fill(CHARS, 2979, 2981, -19.toByte)

  Arrays.fill(CHARS, 2981, 2984, 33.toByte)

  Arrays.fill(CHARS, 2984, 2987, -19.toByte)

  Arrays.fill(CHARS, 2987, 2990, 33.toByte)

  Arrays.fill(CHARS, 2990, 2998, -19.toByte)

  CHARS(2998) = 33

  Arrays.fill(CHARS, 2999, 3002, -19.toByte)

  Arrays.fill(CHARS, 3002, 3006, 33.toByte)

  Arrays.fill(CHARS, 3006, 3011, -87.toByte)

  Arrays.fill(CHARS, 3011, 3014, 33.toByte)

  Arrays.fill(CHARS, 3014, 3017, -87.toByte)

  CHARS(3017) = 33

  Arrays.fill(CHARS, 3018, 3022, -87.toByte)

  Arrays.fill(CHARS, 3022, 3031, 33.toByte)

  CHARS(3031) = -87

  Arrays.fill(CHARS, 3032, 3047, 33.toByte)

  Arrays.fill(CHARS, 3047, 3056, -87.toByte)

  Arrays.fill(CHARS, 3056, 3073, 33.toByte)

  Arrays.fill(CHARS, 3073, 3076, -87.toByte)

  CHARS(3076) = 33

  Arrays.fill(CHARS, 3077, 3085, -19.toByte)

  CHARS(3085) = 33

  Arrays.fill(CHARS, 3086, 3089, -19.toByte)

  CHARS(3089) = 33

  Arrays.fill(CHARS, 3090, 3113, -19.toByte)

  CHARS(3113) = 33

  Arrays.fill(CHARS, 3114, 3124, -19.toByte)

  CHARS(3124) = 33

  Arrays.fill(CHARS, 3125, 3130, -19.toByte)

  Arrays.fill(CHARS, 3130, 3134, 33.toByte)

  Arrays.fill(CHARS, 3134, 3141, -87.toByte)

  CHARS(3141) = 33

  Arrays.fill(CHARS, 3142, 3145, -87.toByte)

  CHARS(3145) = 33

  Arrays.fill(CHARS, 3146, 3150, -87.toByte)

  Arrays.fill(CHARS, 3150, 3157, 33.toByte)

  Arrays.fill(CHARS, 3157, 3159, -87.toByte)

  Arrays.fill(CHARS, 3159, 3168, 33.toByte)

  Arrays.fill(CHARS, 3168, 3170, -19.toByte)

  Arrays.fill(CHARS, 3170, 3174, 33.toByte)

  Arrays.fill(CHARS, 3174, 3184, -87.toByte)

  Arrays.fill(CHARS, 3184, 3202, 33.toByte)

  Arrays.fill(CHARS, 3202, 3204, -87.toByte)

  CHARS(3204) = 33

  Arrays.fill(CHARS, 3205, 3213, -19.toByte)

  CHARS(3213) = 33

  Arrays.fill(CHARS, 3214, 3217, -19.toByte)

  CHARS(3217) = 33

  Arrays.fill(CHARS, 3218, 3241, -19.toByte)

  CHARS(3241) = 33

  Arrays.fill(CHARS, 3242, 3252, -19.toByte)

  CHARS(3252) = 33

  Arrays.fill(CHARS, 3253, 3258, -19.toByte)

  Arrays.fill(CHARS, 3258, 3262, 33.toByte)

  Arrays.fill(CHARS, 3262, 3269, -87.toByte)

  CHARS(3269) = 33

  Arrays.fill(CHARS, 3270, 3273, -87.toByte)

  CHARS(3273) = 33

  Arrays.fill(CHARS, 3274, 3278, -87.toByte)

  Arrays.fill(CHARS, 3278, 3285, 33.toByte)

  Arrays.fill(CHARS, 3285, 3287, -87.toByte)

  Arrays.fill(CHARS, 3287, 3294, 33.toByte)

  CHARS(3294) = -19

  CHARS(3295) = 33

  Arrays.fill(CHARS, 3296, 3298, -19.toByte)

  Arrays.fill(CHARS, 3298, 3302, 33.toByte)

  Arrays.fill(CHARS, 3302, 3312, -87.toByte)

  Arrays.fill(CHARS, 3312, 3330, 33.toByte)

  Arrays.fill(CHARS, 3330, 3332, -87.toByte)

  CHARS(3332) = 33

  Arrays.fill(CHARS, 3333, 3341, -19.toByte)

  CHARS(3341) = 33

  Arrays.fill(CHARS, 3342, 3345, -19.toByte)

  CHARS(3345) = 33

  Arrays.fill(CHARS, 3346, 3369, -19.toByte)

  CHARS(3369) = 33

  Arrays.fill(CHARS, 3370, 3386, -19.toByte)

  Arrays.fill(CHARS, 3386, 3390, 33.toByte)

  Arrays.fill(CHARS, 3390, 3396, -87.toByte)

  Arrays.fill(CHARS, 3396, 3398, 33.toByte)

  Arrays.fill(CHARS, 3398, 3401, -87.toByte)

  CHARS(3401) = 33

  Arrays.fill(CHARS, 3402, 3406, -87.toByte)

  Arrays.fill(CHARS, 3406, 3415, 33.toByte)

  CHARS(3415) = -87

  Arrays.fill(CHARS, 3416, 3424, 33.toByte)

  Arrays.fill(CHARS, 3424, 3426, -19.toByte)

  Arrays.fill(CHARS, 3426, 3430, 33.toByte)

  Arrays.fill(CHARS, 3430, 3440, -87.toByte)

  Arrays.fill(CHARS, 3440, 3585, 33.toByte)

  Arrays.fill(CHARS, 3585, 3631, -19.toByte)

  CHARS(3631) = 33

  CHARS(3632) = -19

  CHARS(3633) = -87

  Arrays.fill(CHARS, 3634, 3636, -19.toByte)

  Arrays.fill(CHARS, 3636, 3643, -87.toByte)

  Arrays.fill(CHARS, 3643, 3648, 33.toByte)

  Arrays.fill(CHARS, 3648, 3654, -19.toByte)

  Arrays.fill(CHARS, 3654, 3663, -87.toByte)

  CHARS(3663) = 33

  Arrays.fill(CHARS, 3664, 3674, -87.toByte)

  Arrays.fill(CHARS, 3674, 3713, 33.toByte)

  Arrays.fill(CHARS, 3713, 3715, -19.toByte)

  CHARS(3715) = 33

  CHARS(3716) = -19

  Arrays.fill(CHARS, 3717, 3719, 33.toByte)

  Arrays.fill(CHARS, 3719, 3721, -19.toByte)

  CHARS(3721) = 33

  CHARS(3722) = -19

  Arrays.fill(CHARS, 3723, 3725, 33.toByte)

  CHARS(3725) = -19

  Arrays.fill(CHARS, 3726, 3732, 33.toByte)

  Arrays.fill(CHARS, 3732, 3736, -19.toByte)

  CHARS(3736) = 33

  Arrays.fill(CHARS, 3737, 3744, -19.toByte)

  CHARS(3744) = 33

  Arrays.fill(CHARS, 3745, 3748, -19.toByte)

  CHARS(3748) = 33

  CHARS(3749) = -19

  CHARS(3750) = 33

  CHARS(3751) = -19

  Arrays.fill(CHARS, 3752, 3754, 33.toByte)

  Arrays.fill(CHARS, 3754, 3756, -19.toByte)

  CHARS(3756) = 33

  Arrays.fill(CHARS, 3757, 3759, -19.toByte)

  CHARS(3759) = 33

  CHARS(3760) = -19

  CHARS(3761) = -87

  Arrays.fill(CHARS, 3762, 3764, -19.toByte)

  Arrays.fill(CHARS, 3764, 3770, -87.toByte)

  CHARS(3770) = 33

  Arrays.fill(CHARS, 3771, 3773, -87.toByte)

  CHARS(3773) = -19

  Arrays.fill(CHARS, 3774, 3776, 33.toByte)

  Arrays.fill(CHARS, 3776, 3781, -19.toByte)

  CHARS(3781) = 33

  CHARS(3782) = -87

  CHARS(3783) = 33

  Arrays.fill(CHARS, 3784, 3790, -87.toByte)

  Arrays.fill(CHARS, 3790, 3792, 33.toByte)

  Arrays.fill(CHARS, 3792, 3802, -87.toByte)

  Arrays.fill(CHARS, 3802, 3864, 33.toByte)

  Arrays.fill(CHARS, 3864, 3866, -87.toByte)

  Arrays.fill(CHARS, 3866, 3872, 33.toByte)

  Arrays.fill(CHARS, 3872, 3882, -87.toByte)

  Arrays.fill(CHARS, 3882, 3893, 33.toByte)

  CHARS(3893) = -87

  CHARS(3894) = 33

  CHARS(3895) = -87

  CHARS(3896) = 33

  CHARS(3897) = -87

  Arrays.fill(CHARS, 3898, 3902, 33.toByte)

  Arrays.fill(CHARS, 3902, 3904, -87.toByte)

  Arrays.fill(CHARS, 3904, 3912, -19.toByte)

  CHARS(3912) = 33

  Arrays.fill(CHARS, 3913, 3946, -19.toByte)

  Arrays.fill(CHARS, 3946, 3953, 33.toByte)

  Arrays.fill(CHARS, 3953, 3973, -87.toByte)

  CHARS(3973) = 33

  Arrays.fill(CHARS, 3974, 3980, -87.toByte)

  Arrays.fill(CHARS, 3980, 3984, 33.toByte)

  Arrays.fill(CHARS, 3984, 3990, -87.toByte)

  CHARS(3990) = 33

  CHARS(3991) = -87

  CHARS(3992) = 33

  Arrays.fill(CHARS, 3993, 4014, -87.toByte)

  Arrays.fill(CHARS, 4014, 4017, 33.toByte)

  Arrays.fill(CHARS, 4017, 4024, -87.toByte)

  CHARS(4024) = 33

  CHARS(4025) = -87

  Arrays.fill(CHARS, 4026, 4256, 33.toByte)

  Arrays.fill(CHARS, 4256, 4294, -19.toByte)

  Arrays.fill(CHARS, 4294, 4304, 33.toByte)

  Arrays.fill(CHARS, 4304, 4343, -19.toByte)

  Arrays.fill(CHARS, 4343, 4352, 33.toByte)

  CHARS(4352) = -19

  CHARS(4353) = 33

  Arrays.fill(CHARS, 4354, 4356, -19.toByte)

  CHARS(4356) = 33

  Arrays.fill(CHARS, 4357, 4360, -19.toByte)

  CHARS(4360) = 33

  CHARS(4361) = -19

  CHARS(4362) = 33

  Arrays.fill(CHARS, 4363, 4365, -19.toByte)

  CHARS(4365) = 33

  Arrays.fill(CHARS, 4366, 4371, -19.toByte)

  Arrays.fill(CHARS, 4371, 4412, 33.toByte)

  CHARS(4412) = -19

  CHARS(4413) = 33

  CHARS(4414) = -19

  CHARS(4415) = 33

  CHARS(4416) = -19

  Arrays.fill(CHARS, 4417, 4428, 33.toByte)

  CHARS(4428) = -19

  CHARS(4429) = 33

  CHARS(4430) = -19

  CHARS(4431) = 33

  CHARS(4432) = -19

  Arrays.fill(CHARS, 4433, 4436, 33.toByte)

  Arrays.fill(CHARS, 4436, 4438, -19.toByte)

  Arrays.fill(CHARS, 4438, 4441, 33.toByte)

  CHARS(4441) = -19

  Arrays.fill(CHARS, 4442, 4447, 33.toByte)

  Arrays.fill(CHARS, 4447, 4450, -19.toByte)

  CHARS(4450) = 33

  CHARS(4451) = -19

  CHARS(4452) = 33

  CHARS(4453) = -19

  CHARS(4454) = 33

  CHARS(4455) = -19

  CHARS(4456) = 33

  CHARS(4457) = -19

  Arrays.fill(CHARS, 4458, 4461, 33.toByte)

  Arrays.fill(CHARS, 4461, 4463, -19.toByte)

  Arrays.fill(CHARS, 4463, 4466, 33.toByte)

  Arrays.fill(CHARS, 4466, 4468, -19.toByte)

  CHARS(4468) = 33

  CHARS(4469) = -19

  Arrays.fill(CHARS, 4470, 4510, 33.toByte)

  CHARS(4510) = -19

  Arrays.fill(CHARS, 4511, 4520, 33.toByte)

  CHARS(4520) = -19

  Arrays.fill(CHARS, 4521, 4523, 33.toByte)

  CHARS(4523) = -19

  Arrays.fill(CHARS, 4524, 4526, 33.toByte)

  Arrays.fill(CHARS, 4526, 4528, -19.toByte)

  Arrays.fill(CHARS, 4528, 4535, 33.toByte)

  Arrays.fill(CHARS, 4535, 4537, -19.toByte)

  CHARS(4537) = 33

  CHARS(4538) = -19

  CHARS(4539) = 33

  Arrays.fill(CHARS, 4540, 4547, -19.toByte)

  Arrays.fill(CHARS, 4547, 4587, 33.toByte)

  CHARS(4587) = -19

  Arrays.fill(CHARS, 4588, 4592, 33.toByte)

  CHARS(4592) = -19

  Arrays.fill(CHARS, 4593, 4601, 33.toByte)

  CHARS(4601) = -19

  Arrays.fill(CHARS, 4602, 7680, 33.toByte)

  Arrays.fill(CHARS, 7680, 7836, -19.toByte)

  Arrays.fill(CHARS, 7836, 7840, 33.toByte)

  Arrays.fill(CHARS, 7840, 7930, -19.toByte)

  Arrays.fill(CHARS, 7930, 7936, 33.toByte)

  Arrays.fill(CHARS, 7936, 7958, -19.toByte)

  Arrays.fill(CHARS, 7958, 7960, 33.toByte)

  Arrays.fill(CHARS, 7960, 7966, -19.toByte)

  Arrays.fill(CHARS, 7966, 7968, 33.toByte)

  Arrays.fill(CHARS, 7968, 8006, -19.toByte)

  Arrays.fill(CHARS, 8006, 8008, 33.toByte)

  Arrays.fill(CHARS, 8008, 8014, -19.toByte)

  Arrays.fill(CHARS, 8014, 8016, 33.toByte)

  Arrays.fill(CHARS, 8016, 8024, -19.toByte)

  CHARS(8024) = 33

  CHARS(8025) = -19

  CHARS(8026) = 33

  CHARS(8027) = -19

  CHARS(8028) = 33

  CHARS(8029) = -19

  CHARS(8030) = 33

  Arrays.fill(CHARS, 8031, 8062, -19.toByte)

  Arrays.fill(CHARS, 8062, 8064, 33.toByte)

  Arrays.fill(CHARS, 8064, 8117, -19.toByte)

  CHARS(8117) = 33

  Arrays.fill(CHARS, 8118, 8125, -19.toByte)

  CHARS(8125) = 33

  CHARS(8126) = -19

  Arrays.fill(CHARS, 8127, 8130, 33.toByte)

  Arrays.fill(CHARS, 8130, 8133, -19.toByte)

  CHARS(8133) = 33

  Arrays.fill(CHARS, 8134, 8141, -19.toByte)

  Arrays.fill(CHARS, 8141, 8144, 33.toByte)

  Arrays.fill(CHARS, 8144, 8148, -19.toByte)

  Arrays.fill(CHARS, 8148, 8150, 33.toByte)

  Arrays.fill(CHARS, 8150, 8156, -19.toByte)

  Arrays.fill(CHARS, 8156, 8160, 33.toByte)

  Arrays.fill(CHARS, 8160, 8173, -19.toByte)

  Arrays.fill(CHARS, 8173, 8178, 33.toByte)

  Arrays.fill(CHARS, 8178, 8181, -19.toByte)

  CHARS(8181) = 33

  Arrays.fill(CHARS, 8182, 8189, -19.toByte)

  Arrays.fill(CHARS, 8189, 8400, 33.toByte)

  Arrays.fill(CHARS, 8400, 8413, -87.toByte)

  Arrays.fill(CHARS, 8413, 8417, 33.toByte)

  CHARS(8417) = -87

  Arrays.fill(CHARS, 8418, 8486, 33.toByte)

  CHARS(8486) = -19

  Arrays.fill(CHARS, 8487, 8490, 33.toByte)

  Arrays.fill(CHARS, 8490, 8492, -19.toByte)

  Arrays.fill(CHARS, 8492, 8494, 33.toByte)

  CHARS(8494) = -19

  Arrays.fill(CHARS, 8495, 8576, 33.toByte)

  Arrays.fill(CHARS, 8576, 8579, -19.toByte)

  Arrays.fill(CHARS, 8579, 12293, 33.toByte)

  CHARS(12293) = -87

  CHARS(12294) = 33

  CHARS(12295) = -19

  Arrays.fill(CHARS, 12296, 12321, 33.toByte)

  Arrays.fill(CHARS, 12321, 12330, -19.toByte)

  Arrays.fill(CHARS, 12330, 12336, -87.toByte)

  CHARS(12336) = 33

  Arrays.fill(CHARS, 12337, 12342, -87.toByte)

  Arrays.fill(CHARS, 12342, 12353, 33.toByte)

  Arrays.fill(CHARS, 12353, 12437, -19.toByte)

  Arrays.fill(CHARS, 12437, 12441, 33.toByte)

  Arrays.fill(CHARS, 12441, 12443, -87.toByte)

  Arrays.fill(CHARS, 12443, 12445, 33.toByte)

  Arrays.fill(CHARS, 12445, 12447, -87.toByte)

  Arrays.fill(CHARS, 12447, 12449, 33.toByte)

  Arrays.fill(CHARS, 12449, 12539, -19.toByte)

  CHARS(12539) = 33

  Arrays.fill(CHARS, 12540, 12543, -87.toByte)

  Arrays.fill(CHARS, 12543, 12549, 33.toByte)

  Arrays.fill(CHARS, 12549, 12589, -19.toByte)

  Arrays.fill(CHARS, 12589, 19968, 33.toByte)

  Arrays.fill(CHARS, 19968, 40870, -19.toByte)

  Arrays.fill(CHARS, 40870, 44032, 33.toByte)

  Arrays.fill(CHARS, 44032, 55204, -19.toByte)

  Arrays.fill(CHARS, 55204, 55296, 33.toByte)

  Arrays.fill(CHARS, 57344, 65534, 33.toByte)

  /**
   * Returns true if the specified character is a supplemental character.
   *
   * @param c The character to check.
   */
  def isSupplemental(c: Int): Boolean = c >= 0x10000 && c <= 0x10FFFF

  /**
   * Returns true the supplemental character corresponding to the given
   * surrogates.
   *
   * @param h The high surrogate.
   * @param l The low surrogate.
   */
  def supplemental(h: Char, l: Char): Int = {
    (h - 0xD800) * 0x400 + (l - 0xDC00) + 0x10000
  }

  /**
   * Returns the high surrogate of a supplemental character
   *
   * @param c The supplemental character to "split".
   */
  def highSurrogate(c: Int): Char = {
    (((c - 0x00010000) >> 10) + 0xD800).toChar
  }

  /**
   * Returns the low surrogate of a supplemental character
   *
   * @param c The supplemental character to "split".
   */
  def lowSurrogate(c: Int): Char = {
    (((c - 0x00010000) & 0x3FF) + 0xDC00).toChar
  }

  /**
   * Returns whether the given character is a high surrogate
   *
   * @param c The character to check.
   */
  def isHighSurrogate(c: Int): Boolean = 0xD800 <= c && c <= 0xDBFF

  /**
   * Returns whether the given character is a low surrogate
   *
   * @param c The character to check.
   */
  def isLowSurrogate(c: Int): Boolean = 0xDC00 <= c && c <= 0xDFFF

  /**
   * Returns true if the specified character is valid. This method
   * also checks the surrogate character range from 0x10000 to 0x10FFFF.
   *
   * If the program chooses to apply the mask directly to the
   * `CHARS` array, then they are responsible for checking
   * the surrogate character range.
   *
   * @param c The character to check.
   */
  def isValid(c: Int): Boolean = {
    (c < 0x10000 && (CHARS(c) & MASK_VALID) != 0) || (0x10000 <= c && c <= 0x10FFFF)
  }

  /**
   * Returns true if the specified character is invalid.
   *
   * @param c The character to check.
   */
  def isInvalid(c: Int): Boolean = !isValid(c)

  /**
   * Returns true if the specified character can be considered content.
   *
   * @param c The character to check.
   */
  def isContent(c: Int): Boolean = {
    (c < 0x10000 && (CHARS(c) & MASK_CONTENT) != 0) || (0x10000 <= c && c <= 0x10FFFF)
  }

  /**
   * Returns true if the specified character can be considered markup.
   * Markup characters include '<', '&', and '%'.
   *
   * @param c The character to check.
   */
  def isMarkup(c: Int): Boolean = c == '<' || c == '&' || c == '%'

  /**
   * Returns true if the specified character is a space character
   * as defined by production [3] in the XML 1.0 specification.
   *
   * @param c The character to check.
   */
  def isSpace(c: Int): Boolean = {
    c <= 0x20 && (CHARS(c) & MASK_SPACE) != 0
  }

  /**
   * Returns true if the specified character is a valid name start
   * character as defined by production [5] in the XML 1.0
   * specification.
   *
   * @param c The character to check.
   */
  def isNameStart(c: Int): Boolean = {
    c < 0x10000 && (CHARS(c) & MASK_NAME_START) != 0
  }

  /**
   * Returns true if the specified character is a valid name
   * character as defined by production [4] in the XML 1.0
   * specification.
   *
   * @param c The character to check.
   */
  def isName(c: Int): Boolean = {
    c < 0x10000 && (CHARS(c) & MASK_NAME) != 0
  }

  /**
   * Returns true if the specified character is a valid NCName start
   * character as defined by production [4] in Namespaces in XML
   * recommendation.
   *
   * @param c The character to check.
   */
  def isNCNameStart(c: Int): Boolean = {
    c < 0x10000 && (CHARS(c) & MASK_NCNAME_START) != 0
  }

  /**
   * Returns true if the specified character is a valid NCName
   * character as defined by production [5] in Namespaces in XML
   * recommendation.
   *
   * @param c The character to check.
   */
  def isNCName(c: Int): Boolean = {
    c < 0x10000 && (CHARS(c) & MASK_NCNAME) != 0
  }

  /**
   * Returns true if the specified character is a valid Pubid
   * character as defined by production [13] in the XML 1.0
   * specification.
   *
   * @param c The character to check.
   */
  def isPubid(c: Int): Boolean = {
    c < 0x10000 && (CHARS(c) & MASK_PUBID) != 0
  }

  /**
   * Check to see if a string is a valid Name according to [5]
   * in the XML 1.0 Recommendation
   *
   * @param name string to check
   * @return true if name is a valid Name
   */
  def isValidName(name: String): Boolean = {
    val length = name.length
    if (length == 0) {
      return false
    }
    var ch = name.charAt(0)
    if (!isNameStart(ch)) {
      return false
    }
    for (i <- 1 until length) {
      ch = name.charAt(i)
      if (!isName(ch)) {
        return false
      }
    }
    true
  }

  /**
   * Check to see if a string is a valid NCName according to [4]
   * from the XML Namespaces 1.0 Recommendation
   *
   * @param ncName string to check
   * @return true if name is a valid NCName
   */
  def isValidNCName(ncName: String): Boolean = {
    val length = ncName.length
    if (length == 0) {
      return false
    }
    var ch = ncName.charAt(0)
    if (!isNCNameStart(ch)) {
      return false
    }
    for (i <- 1 until length) {
      ch = ncName.charAt(i)
      if (!isNCName(ch)) {
        return false
      }
    }
    true
  }

  /**
   * Check to see if a string is a valid Nmtoken according to [7]
   * in the XML 1.0 Recommendation
   *
   * @param nmtoken string to check
   * @return true if nmtoken is a valid Nmtoken
   */
  def isValidNmtoken(nmtoken: String): Boolean = {
    val length = nmtoken.length
    if (length == 0) {
      return false
    }
    for (i <- 0 until length) {
      val ch = nmtoken.charAt(i)
      if (!isName(ch)) {
        return false
      }
    }
    true
  }

  /**
   * Returns true if the encoding name is a valid IANA encoding.
   * This method does not verify that there is a decoder available
   * for this encoding, only that the characters are valid for an
   * IANA encoding name.
   *
   * @param ianaEncoding The IANA encoding name.
   */
  def isValidIANAEncoding(ianaEncoding: String): Boolean = {
    if (ianaEncoding ne null) {
      val length = ianaEncoding.length
      if (length > 0) {
        var c = ianaEncoding.charAt(0)
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
          for (i <- 1 until length) {
            c = ianaEncoding.charAt(i)
            if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') &&
              c != '.' &&
              c != '_' &&
              c != '-') {
              return false
            }
          }
          return true
        }
      }
    }
    false
  }

  /**
   * Returns true if the encoding name is a valid Java encoding.
   * This method does not verify that there is a decoder available
   * for this encoding, only that the characters are valid for an
   * Java encoding name.
   *
   * @param javaEncoding The Java encoding name.
   */
  def isValidJavaEncoding(javaEncoding: String): Boolean = {
    if (javaEncoding ne null) {
      val length = javaEncoding.length
      if (length > 0) {
        for (i <- 1 until length) {
          val c = javaEncoding.charAt(i)
          if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') &&
            c != '.' &&
            c != '_' &&
            c != '-') {
            return false
          }
        }
        return true
      }
    }
    false
  }

  /**
   * Trims space characters as defined by production [3] in
   * the XML 1.0 specification from both ends of the given string.
   *
   * @param value the string to be trimmed
   * @return the given string with the space characters trimmed
   * from both ends
   */
  def trim(value: String): String = {
    var start: Int = 0
    var end: Int = 0
    val lengthMinusOne = value.length - 1
    start = 0
    locally {
      var exitLoop = false
      while (! exitLoop && start <= lengthMinusOne)
        if (! isSpace(value.charAt(start)))
          exitLoop = true
        else
          start += 1
    }
    end = lengthMinusOne
    locally {
      var exitLoop = false
      while (! exitLoop && end >= start) {
        if (! isSpace(value.charAt(end)))
          exitLoop = true
        else
          end -= 1
      }
    }
    if (start == 0 && end == lengthMinusOne)
      return value
    if (start > lengthMinusOne)
      return ""
    value.substring(start, end + 1)
  }
}
