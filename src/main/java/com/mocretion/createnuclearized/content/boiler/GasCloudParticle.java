package com.mocretion.createnuclearized.content.boiler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PlayerCloudParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;

public class GasCloudParticle extends PlayerCloudParticle {

    protected GasCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet sprites, ColorParticleOption color) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        setColor(rCol * color.getRed(), gCol * color.getGreen(), bCol * color.getBlue());
        setAlpha(color.getAlpha());
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {

        @Override
        public Particle createParticle(ColorParticleOption color, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new GasCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, color);
        }
    }
}
