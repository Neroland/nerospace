import unittest
from pathlib import Path

import numpy as np
from PIL import Image

import gen_sky_textures as sky


class SkyTextureTest(unittest.TestCase):
    def test_shipped_nebulae_keep_vivid_distinct_palettes(self):
        root = Path(__file__).resolve().parent.parent / sky.ASSET_DIR / 'sky'
        colors = []
        for name in ['nebula_a', 'nebula_b']:
            with Image.open(root / (name + '.png')) as image:
                pixels = np.asarray(image)
            visible = pixels[..., :3][pixels[..., 3] > 40].astype(np.float32) / 255
            saturation = (visible.max(axis=1) - visible.min(axis=1)) / np.maximum(visible.max(axis=1), 0.001)
            self.assertGreater(np.median(saturation), 0.4, name)
            colors.append(visible.mean(axis=0))
        self.assertGreater(np.linalg.norm(colors[0] - colors[1]), 0.3)

    def test_noise_is_continuous_and_reproducible(self):
        a = sky.noise_space('volume-test')
        b = sky.noise_space('volume-test')
        x = np.linspace(-2, 2, 64, dtype=np.float32)
        np.testing.assert_array_equal(a(x, x, x), b(x, x, x))
        self.assertLess(np.max(np.abs(a(x, x, x) - a(x + 1e-4, x, x))), 0.001)
        self.assertTrue(np.isfinite(a(x, x, x)).all())

    def test_invalid_octaves_fail_explicitly(self):
        for octaves in [0, -1, 9]:
            with self.assertRaises(ValueError):
                sky.volume_fbm(sky.noise_space('invalid'), 0, 0, 0, octaves)

    def test_band_tiles_without_a_seam_or_a_hard_top(self):
        band = np.asarray(sky.galaxy_band('seam', 128, 64, '#DDCCAA', '#667788', '#101020', '#FFFFFF'))
        np.testing.assert_array_equal(band[:, 0], band[:, -1])
        self.assertEqual(0, band[0, :, 3].max())
        self.assertEqual(0, band[-1, :, 3].max())
        self.assertGreater(np.count_nonzero(band[..., 3]), 128)

    def test_every_star_sprite_is_visible_even_on_an_even_sized_grid(self):
        for sheet in [sky.star_sheet(), sky.star_sheet_natural()]:
            alpha = np.asarray(sheet)[..., 3]
            for y in range(4):
                for x in range(4):
                    self.assertGreater(alpha[y * 16:(y + 1) * 16, x * 16:(x + 1) * 16].max(), 20)

    def test_planet_disc_and_lights_share_the_land_mask(self):
        rgb, alpha, land = sky.earth_parts('earth', 96, baked_light=False)
        lights = np.asarray(sky.city_lights('lights', 96, 'earth'))[..., 3]
        self.assertTrue(np.isfinite(rgb).all())
        self.assertEqual(0, alpha[0, 0])
        self.assertGreater(alpha[48, 48], 0.99)
        self.assertEqual(0, lights[land == 0].max())

    def test_filaments_fade_before_the_quad_boundary(self):
        image = np.asarray(sky.nebula_patch('edge', 96, ['#BB8866', '#556677', '#DDDDBB']))
        self.assertEqual(0, image[0, :, 3].max())
        self.assertEqual(0, image[:, -1, 3].max())
        self.assertGreater(image[..., 3].max(), 40)

    def _jupiter(self, frame):
        return np.asarray(sky.gas_giant('jupiter', sky.JUPITER_FRAME_SIZE, frame=frame, frames=sky.JUPITER_FRAMES),
                          dtype=np.float32)

    def test_jupiter_drift_loop_closes(self):
        # frame `frames` is rendered without wrapping: the 45-degree drift and the 45-degree periodic turbulence
        # bring it back to frame 0 (up to one level of rounding).
        first = self._jupiter(0)
        closing = self._jupiter(sky.JUPITER_FRAMES)
        self.assertLessEqual(np.max(np.abs(first - closing)), 1.0)

    def test_jupiter_neighbouring_frames_differ_slightly(self):
        frames = [self._jupiter(i) for i in range(sky.JUPITER_FRAMES)] + [self._jupiter(0)]
        moved = 0.0
        for a, b in zip(frames, frames[1:]):
            diff = np.mean(np.abs(a - b))
            self.assertLess(diff, 6.0)
            moved += diff
        self.assertGreater(moved, 0.0)

    def test_jupiter_frames_are_shipped_with_square_variants(self):
        root = Path(__file__).resolve().parent.parent / sky.ASSET_DIR / 'sky'
        for i in range(sky.JUPITER_FRAMES):
            name = 'jupiter_%02d.png' % i
            self.assertTrue((root / name).is_file(), name)
            self.assertTrue((root / 'square' / name).is_file(), 'square/' + name)
        self.assertTrue((root / 'jupiter.png').is_file())

    # ---- square body style: flat, hard-edged squares at the globe's resolution ----

    def _alpha(self, image):
        return np.array(image.convert('RGBA'))[..., 3]

    def test_square_variants_are_shipped_at_the_globe_resolution(self):
        root = Path(__file__).resolve().parent.parent / sky.ASSET_DIR / 'sky'
        names = list(sky.SQUARE_BODIES) + ['terminator_%02d' % i for i in range(sky.TERMINATOR_FRAMES)]
        for name in names:
            with Image.open(root / (name + '.png')) as globe, Image.open(root / 'square' / (name + '.png')) as square:
                self.assertEqual(globe.size, square.size, name)
                self.assertGreaterEqual(square.width, 64, name)

    def test_square_body_is_an_opaque_hard_edged_square_of_the_disc_half_size(self):
        for name, radius in [('moon_small', 0.8), ('phobos', 0.7), ('sun_small', 0.42)]:
            image = sky.sky_textures(flat=True)[name]()
            alpha = self._alpha(image)
            size = image.width
            half = int(round(radius * size / 2.0))
            lo, hi = size // 2 - half, size // 2 + half
            self.assertTrue(set(np.unique(alpha)) <= {0, 255}, name)
            self.assertEqual(255, alpha[lo:hi, lo:hi].min(), name)
            alpha[lo:hi, lo:hi] = 0
            self.assertEqual(0, alpha.max(), name + ' draws outside its square')

    def test_square_body_has_no_limb_darkening(self):
        for flat in (True, False):
            rgb = np.asarray(sky.gas_giant('jupiter', 128, flat=flat).convert('RGB'), dtype=np.float32).mean(axis=2)
            half = int(round(sky.JUPITER_RADIUS * 64))
            lo, hi = 64 - half, 64 + half
            edge = np.concatenate([rgb[lo:hi, lo:lo + 6].ravel(), rgb[lo:hi, hi - 6:hi].ravel()]).mean()
            centre = rgb[54:74, 54:74].mean()
            if flat:
                self.assertGreater(edge / centre, 0.8)
            else:
                self.assertLess(edge / centre, 0.6, 'the globe keeps its limb darkening')

    def test_square_jupiter_drift_loop_closes(self):
        first = np.asarray(sky.gas_giant('jupiter', sky.JUPITER_FRAME_SIZE, frame=0, frames=sky.JUPITER_FRAMES,
                                         flat=True), dtype=np.float32)
        closing = np.asarray(sky.gas_giant('jupiter', sky.JUPITER_FRAME_SIZE, frame=sky.JUPITER_FRAMES,
                                           frames=sky.JUPITER_FRAMES, flat=True), dtype=np.float32)
        self.assertLessEqual(np.max(np.abs(first - closing)), 1.0)

    def test_square_night_shade_is_hard_edged_and_square_lights_keep_their_mask(self):
        frame = sky.terminator_phase('terminator_07', 7 / 15.0)
        shade = self._alpha(sky.square_shade(frame))
        self.assertEqual(frame.size, (shade.shape[1], shade.shape[0]))
        self.assertTrue(set(np.unique(shade)) <= {0, 255})
        self.assertGreater(np.count_nonzero(shade), 0)
        lights = self._alpha(sky.city_lights('lights', 96, 'earth', flat=True))
        self.assertGreater(lights.max(), 0)
        self.assertGreater(np.count_nonzero((lights > 0) & (lights < 255)), 0, 'the lights keep a soft alpha mask')


if __name__ == '__main__':
    unittest.main()
