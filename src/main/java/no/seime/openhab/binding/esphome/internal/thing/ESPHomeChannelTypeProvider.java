/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * largely based on OpenHAB's src/main/java/org/openhab/core/thing/DefaultSystemChannelTypeProvider.java
 */
package no.seime.openhab.binding.esphome.internal.thing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocalizedKey;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation providing standard ESPHome channel types
 *
 * @author Dave Corder - Initial contribution
 */
@NonNullByDefault
@Component
public class ESPHomeChannelTypeProvider implements ChannelTypeProvider {

    static final String BINDING_ID = "esphome";

    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_ON_OFF = new ChannelTypeUID(BINDING_ID, "on_off");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS = new ChannelTypeUID(BINDING_ID,
            "brightness");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_WHITE = new ChannelTypeUID(BINDING_ID, "white");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_COLOR = new ChannelTypeUID(BINDING_ID, "color");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE = new ChannelTypeUID(BINDING_ID,
            "color-temperature");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE = new ChannelTypeUID(BINDING_ID,
            "color-mode");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE = new ChannelTypeUID(BINDING_ID,
            "warm-white");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE = new ChannelTypeUID(BINDING_ID,
            "cold-white");
    public static final ChannelTypeUID ESPHOME_CHANNEL_TYPE_UID_EFFECTS = new ChannelTypeUID(BINDING_ID, "effects");

    /**
     * On/Off: default system wide {@link ChannelType} which allows turning the light on and off
     */
    public static final ChannelType ESPHOME_CHANNEL_ON_OFF = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_ON_OFF, "On/Off", CoreItemFactory.SWITCH)
            .withDescription("Switches the light on and off").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            // .withTags(Point.CONTROL, Property.BRIGHTNESS).build(); // OH 5.0
            .withTags(List.of("Switch", "Power")).build();

    /**
     * Brightness: default system wide {@link ChannelType} which allows changing the brightness from 0-100%
     */
    public static final ChannelType ESPHOME_CHANNEL_BRIGHTNESS = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS, "Brightness", CoreItemFactory.DIMMER)
            .withDescription("Controls the brightness of the light").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            // .withTags(Point.CONTROL, Property.BRIGHTNESS).build(); // OH 5.0
            .withTags(List.of("Control", "Light")).build();

    /**
     * White: default system wide {@link ChannelType} which allows changing the brightness of the white channel from
     * 0-100%
     */
    public static final ChannelType ESPHOME_CHANNEL_WHITE = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_WHITE, "White", CoreItemFactory.DIMMER)
            .withDescription("Controls the brightness of the white channel").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            // .withTags(Point.CONTROL, Property.BRIGHTNESS).build(); // OH 5.0
            .withTags(List.of("Control", "Light")).build();

    /**
     * Color: default system wide {@link ChannelType} which allows changing the color
     */
    public static final ChannelType ESPHOME_CHANNEL_COLOR = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_COLOR, "Color", CoreItemFactory.COLOR)
            .withDescription("Controls the color of the light").withCategory("ColorLight")
            // .withTags(Point.CONTROL, Property.COLOR).build(); // OH 5.0
            .withTags(List.of("Control", "Light")).build();

    /**
     * Color-temperature: default system wide {@link ChannelType} which allows changing the color temperature in percent
     */
    public static final ChannelType ESPHOME_CHANNEL_COLOR_TEMPERATURE = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE, "Color Temperature", CoreItemFactory.DIMMER)
            .withDescription("Controls the color temperature of the light from 0 (cold) to 100 (warm)")
            .withCategory("ColorLight")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%.0f").build())
            // .withTags(Point.CONTROL, Property.COLOR_TEMPERATURE).build(); // OH 5.0
            .withTags(List.of("Control", "ColorTemperature")).build();

    /**
     * Color-mode: default system wide {@link ChannelType} which allows changing the color mode (if supported by the
     * light)
     */
    public static final ChannelType ESPHOME_CHANNEL_COLOR_MODE = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE, "Color Mode", CoreItemFactory.STRING)
            .withDescription("Controls the color mode of the light (if supported)").withCategory("ColorLight")
            // .withTags(Point.CONTROL, Property.COLOR_TEMPERATURE).build(); // OH 5.0
            .withTags(List.of("Control", "ColorMode")).build();

    /**
     * Warm White: default system wide {@link ChannelType} which allows changing the brightness of the Warm White
     * channel from 0-100%
     */
    public static final ChannelType ESPHOME_CHANNEL_WARM_WHITE = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE, "Warm White", CoreItemFactory.DIMMER)
            .withDescription("Controls the brightness of the warm white channel").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            // .withTags(Point.CONTROL, Property.BRIGHTNESS).build(); // OH 5.0
            .withTags(List.of("Control", "Light")).build();

    /**
     * Cold White: default system wide {@link ChannelType} which allows changing the brightness of the Cold White
     * channel from 0-100%
     */
    public static final ChannelType ESPHOME_CHANNEL_COLD_WHITE = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE, "Cold White", CoreItemFactory.DIMMER)
            .withDescription("Controls the brightness of the cold white channel").withCategory("Light")
            .withStateDescriptionFragment(StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                    .withMaximum(new BigDecimal(100)).withPattern("%d %%").build())
            // .withTags(Point.CONTROL, Property.BRIGHTNESS).build(); // OH 5.0
            .withTags(List.of("Control", "Light")).build();

    /**
     * Effects: default system wide {@link ChannelType} which allows changing the effect of the light
     */
    public static final ChannelType ESPHOME_CHANNEL_EFFECTS = ChannelTypeBuilder
            .state(ESPHOME_CHANNEL_TYPE_UID_EFFECTS, "Effects", CoreItemFactory.STRING)
            .withDescription("Controls the effect of the light").withCategory("Light")
            .withTags(List.of("Control", "Light")).build();

    private static final Collection<ChannelType> CHANNEL_TYPES = List.of(ESPHOME_CHANNEL_ON_OFF,
            ESPHOME_CHANNEL_BRIGHTNESS, ESPHOME_CHANNEL_WHITE, ESPHOME_CHANNEL_COLOR, ESPHOME_CHANNEL_COLOR_TEMPERATURE,
            ESPHOME_CHANNEL_WARM_WHITE, ESPHOME_CHANNEL_COLD_WHITE);

    private final Map<LocalizedKey, ChannelType> localizedChannelTypeCache = new ConcurrentHashMap<>();

    private final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;
    private final BundleResolver bundleResolver;

    @Activate
    public ESPHomeChannelTypeProvider(
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService,
            final @Reference BundleResolver bundleResolver) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
        this.bundleResolver = bundleResolver;
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        final List<ChannelType> allChannelTypes = new ArrayList<>();
        final Bundle bundle = bundleResolver.resolveBundle(ESPHomeChannelTypeProvider.class);

        for (final ChannelType channelType : CHANNEL_TYPES) {
            allChannelTypes.add(createLocalizedChannelType(bundle, channelType, locale));
        }
        return allChannelTypes;
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        final Bundle bundle = bundleResolver.resolveBundle(ESPHomeChannelTypeProvider.class);

        for (final ChannelType channelType : CHANNEL_TYPES) {
            if (channelTypeUID.equals(channelType.getUID())) {
                return createLocalizedChannelType(bundle, channelType, locale);
            }
        }
        return null;
    }

    private ChannelType createLocalizedChannelType(Bundle bundle, ChannelType channelType, @Nullable Locale locale) {
        LocalizedKey localizedKey = new LocalizedKey(channelType.getUID(),
                locale != null ? locale.toLanguageTag() : null);

        ChannelType cachedEntry = localizedChannelTypeCache.get(localizedKey);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        ChannelType localizedChannelType = channelTypeI18nLocalizationService.createLocalizedChannelType(bundle,
                channelType, locale);
        localizedChannelTypeCache.put(localizedKey, localizedChannelType);
        return localizedChannelType;
    }
}
