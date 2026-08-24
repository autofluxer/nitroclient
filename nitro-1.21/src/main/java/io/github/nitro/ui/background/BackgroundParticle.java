package io.github.nitro.ui.background;

final class BackgroundParticle {
	float x;
	float y;
	float vx;
	float vy;
	float size;
	float life;
	float maxLife;
	int color;
	int type;

	BackgroundParticle reset(float width, float height, int color, int type, float speedMul) {
		this.x = (float) (Math.random() * width);
		this.y = (float) (Math.random() * height);
		this.vx = ((float) Math.random() - 0.5F) * speedMul;
		this.vy = ((float) Math.random() - 0.5F) * speedMul;
		this.size = 1F + (float) Math.random() * 3F;
		this.maxLife = 2F + (float) Math.random() * 4F;
		this.life = this.maxLife;
		this.color = color;
		this.type = type;
		return this;
	}

	boolean tick(float width, float height, float delta) {
		x += vx * delta * 60F;
		y += vy * delta * 60F;
		life -= delta;
		if (type == 1) {
			vy += 0.02F * delta * 60F;
		}
		if (type == 2) {
			vy -= 0.015F * delta * 60F;
		}
		if (x < -8 || x > width + 8 || y < -8 || y > height + 8 || life <= 0F) {
			return false;
		}
		return true;
	}

	float alpha() {
		float fade = life / maxLife;
		return fade * fade;
	}
}
