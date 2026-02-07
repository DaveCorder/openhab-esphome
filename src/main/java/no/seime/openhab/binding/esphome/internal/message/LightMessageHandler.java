package no.seime.openhab.binding.esphome.internal.message;

import static org.openhab.core.library.CoreItemFactory.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateDescription;
import org.openhab.core.util.ColorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.esphome.api.ColorMode;
import io.esphome.api.LightCommandRequest;
import io.esphome.api.LightStateResponse;
import io.esphome.api.ListEntitiesLightResponse;
import no.seime.openhab.binding.esphome.internal.BindingConstants;
import no.seime.openhab.binding.esphome.internal.EntityTypes;
import no.seime.openhab.binding.esphome.internal.comm.ProtocolAPIError;
import no.seime.openhab.binding.esphome.internal.handler.ESPHomeHandler;
import no.seime.openhab.binding.esphome.internal.thing.ESPHomeChannelTypeProvider;

public class LightMessageHandler extends AbstractMessageHandler<ListEntitiesLightResponse, LightStateResponse> {

    private final Logger logger = LoggerFactory.getLogger(LightMessageHandler.class);

    public LightMessageHandler(ESPHomeHandler handler) {
        super(handler);
    }

    private final static String CHANNEL_LIGHT = "light";
    private final static String CHANNEL_EFFECTS = "effects";

    @Override
    public void handleCommand(Channel channel, Command command, int key) throws ProtocolAPIError {

        String subCommand = (String) channel.getConfiguration()
                .get(BindingConstants.CHANNEL_CONFIGURATION_ENTITY_FIELD);

        logger.debug(
                "[{}] DWC: handleCommand in LightMessageHandler:\n channel UID: '{}'\n channelType UID: '{}'\n command classname: '{}'\n key: '{}'\n sub command: '{}'\n full command string: {}",
                handler.getLogPrefix(), channel.getUID(), channel.getChannelTypeUID(), command.getClass().getName(),
                key, subCommand, command.toFullString());
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();

        String bindingID = channelTypeUID.getBindingId();
        String channelID = channelTypeUID.getId();
        String channelAsString = channelTypeUID.getAsString();
        String channelClass = channelTypeUID.getClass().getName();

        logger.debug(
                "[{}] DWC: handleCommand in LightMessageHandler\nchannelTypeUID details:\nbinding ID: '{}'\nchannel ID: '{}'\nchannel as String: '{}'\nchannelClass: '{}'",
                handler.getLogPrefix(), bindingID, channelID, channelAsString, channelClass);
        Set<LightColorCapability> capabilities = deserialize((String) channel.getConfiguration().get("capabilities"));
        logger.debug("[{}] DWC: in LightMessageHandler, capabilities {}", handler.getLogPrefix(),
                capabilities.toString());

        // maybe do ON/OFF up here before checking channel type, because in theory ON/OFF could be tied to any channel?
        if (channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLOR)) {
            logger.debug("[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_COLOR", handler.getLogPrefix());
            // double-check command class and light capabilities
            // ignore Brightness here, process in separate block
            if (command instanceof PercentType pct) {
                logger.debug("[{}] DWC: Command is instanceof PercentType, sending colorBrightness",
                        handler.getLogPrefix());

                // LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key);

                // float[] rgbFloat = [builder.getRed(), builder.getBlue(), builder.getGreen()];

                LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                        .setColorBrightness(pct.floatValue() / PercentType.HUNDRED.floatValue())
                        .setHasColorBrightness(true);

                logger.debug("[{}] DWC: Parameters:\n ColorBrightness: {}", handler.getLogPrefix(),
                        pct.floatValue() / PercentType.HUNDRED.floatValue());
                handler.sendMessage(builder.build());
            }

            if (command instanceof HSBType hsb && capabilities.contains(LightColorCapability.RGB)) {

                logger.debug("[{}] DWC: Command is instanceof HSBType and light has RGB capability",
                        handler.getLogPrefix());

                logger.debug("[{}] DWC: Performing HSB to RGB conversion and sending message to light {}",
                        handler.getLogPrefix(), hsb.toFullString());

                logger.debug("[{}] DWC: HSB values:\n H: {}\n S: {}\n B: {}", handler.getLogPrefix(),
                        hsb.getHue().floatValue(), hsb.getSaturation().floatValue(), hsb.getBrightness().floatValue());

                PercentType[] percentTypes = ColorUtil.hsbToRgbPercent(hsb);

                int[] rgbInt = ColorUtil.hsbToRgb(hsb);

                float red = percentTypes[0].floatValue() / PercentType.HUNDRED.floatValue();
                float green = percentTypes[1].floatValue() / PercentType.HUNDRED.floatValue();
                float blue = percentTypes[2].floatValue() / PercentType.HUNDRED.floatValue();

                logger.debug("[{}] DWC: Converted RGB values:\n R: {}/{}\n G: {}/{}\n B: {}/{}", handler.getLogPrefix(),
                        red, rgbInt[0], green, rgbInt[1], blue, rgbInt[2]);

                // int[] rgb = ColorUtil.hsbToRgb(hsb);

                LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                        .setColorMode(ColorMode.COLOR_MODE_RGB).setHasColorMode(true).setRed(red).setGreen(green)
                        .setBlue(blue).setHasRgb(true).setHasWhite(false)
                        .setBrightness(hsb.getBrightness().floatValue() / 100).setHasBrightness(true).setState(true)
                        .setHasState(true);

                // Adjust brightness
                // wait, no, because the B in HSB is already taken into account with the RGB conversion above?
                // builder.setBrightness(hsb.getBrightness().floatValue() / 100).setHasBrightness(true)
                // .setState(true);

                logger.debug("[{}] DWC: Parameters:\nred: {}\ngreen: {}\nblue: {}\nbrightness: {}",
                        handler.getLogPrefix(), percentTypes[0].floatValue() / PercentType.HUNDRED.floatValue(),
                        percentTypes[1].floatValue() / PercentType.HUNDRED.floatValue(),
                        percentTypes[2].floatValue() / PercentType.HUNDRED.floatValue(),
                        hsb.getBrightness().floatValue() / 100);
                handler.sendMessage(builder.build());
            }

        } else if (command instanceof PercentType ww
                && channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE)) {
            logger.debug(
                    "[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE with PercentType float value {}",
                    handler.getLogPrefix(), ww.floatValue());
            LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                    .setColorMode(ColorMode.COLOR_MODE_COLD_WARM_WHITE).setHasColorMode(true)
                    .setWarmWhite(ww.floatValue() / 100).setHasWarmWhite(true).setHasWhite(false).setState(true);

            handler.sendMessage(builder.build());

        } else if (command instanceof PercentType cw
                && channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE)) {
            logger.debug(
                    "[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE with PercentType float value {}",
                    handler.getLogPrefix(), cw.floatValue());
            LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                    .setColorMode(ColorMode.COLOR_MODE_COLD_WARM_WHITE).setHasColorMode(true)
                    .setColdWhite(cw.floatValue() / 100).setHasColdWhite(true).setHasWhite(false).setState(true);

            handler.sendMessage(builder.build());

        } else if (command instanceof PercentType brightness
                && channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS)) {
            logger.debug(
                    "[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS with PercentType float value {}",
                    handler.getLogPrefix(), brightness.floatValue());
            LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                    .setColorMode(ColorMode.COLOR_MODE_COLD_WARM_WHITE).setHasColorMode(true)
                    .setBrightness(brightness.floatValue() / 100).setHasBrightness(true).setState(true)
                    .setHasState(true);

            handler.sendMessage(builder.build());
        } else if (command instanceof PercentType color_temp && channel.getChannelTypeUID()
                .equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE)) {
            logger.debug(
                    "[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE with PercentType float value {}",
                    handler.getLogPrefix(), color_temp.floatValue());
            LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                    .setColorMode(ColorMode.COLOR_MODE_COLOR_TEMPERATURE).setHasColorMode(true)
                    .setColorTemperature(color_temp.floatValue()).setState(true);

            handler.sendMessage(builder.build());
        } else if (channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE)) {
            logger.debug("[{}] DWC: channel Type UID is ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE, command is {}",
                    handler.getLogPrefix(), command);

            ColorMode newColorMode = null;

            // if (command.toString() == "COLOR_MODE_RBG") {
            // newColorMode = ColorMode.COLOR_MODE_RGB;
            try {
                newColorMode = ColorMode.valueOf(command.toString());

                LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                        .setColorMode(newColorMode).setHasColorMode(true).setState(true);

                handler.sendMessage(builder.build());
            } catch (IllegalArgumentException e) {
                logger.debug("[{}] DWC: Unknown color mode '{}'", handler.getLogPrefix(), command.toString());
            }
            // }
        } else if (channel.getChannelTypeUID().equals(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_ON_OFF)) {
            OnOffType onOff = OnOffType.valueOf(command.toString());

            logger.debug("[{}] DWC: Got On/Off command: {} ", handler.getLogPrefix(),
                    onOff.equals(OnOffType.ON) ? "ON" : "OFF");

            LightCommandRequest.Builder builder = LightCommandRequest.newBuilder().setKey(key)
                    .setState(onOff.equals(OnOffType.ON)).setHasState(true);

            handler.sendMessage(builder.build());
        } else {
            logger.warn("[{}] Unsupported command {} for channel {}", handler.getLogPrefix(), command,
                    channel.getUID());
        }
        // case CHANNEL_EFFECTS -> {
        // if (command instanceof StringType stringType) {
        // handler.sendMessage(LightCommandRequest.newBuilder().setKey(key).setEffect(stringType.toString())
        // .setHasEffect(true).setState(true).setHasState(true).build());
        // } else {
        // logger.warn("[{}] Unsupported command {} for channel {}", handler.getLogPrefix(), command,
        // channel.getUID());
        // }
        // }
        // default -> {
        // logger.warn("[{}] Unsupported command/subcommand '{}'/'{}' for channel {}", handler.getLogPrefix(),
        // command, subCommand, channel.getUID());
        // }
        // }
    }

    public void buildChannels(ListEntitiesLightResponse rsp) {
        Configuration configuration = configuration(EntityTypes.LIGHT, rsp.getKey(), CHANNEL_LIGHT);
        SortedSet<LightColorCapability> capabilities = decodeCapabilities(rsp);
        configuration.put("capabilities", serialize(capabilities));

        String icon = getChannelIcon(rsp.getIcon(), "light");

        logger.debug(
                "[{}] DWC: buildChannels() with:\n ListEntitiesLightResponse unique ID '{}'\n" + " response name '{}'\n"
                        + " object ID '{}'",
                handler.getLogPrefix(), rsp.getUniqueId(), rsp.getName(), rsp.getObjectId());

        List<ColorMode> color_modes = rsp.getSupportedColorModesList();
        String color_modes_str;

        color_modes_str = color_modes.stream().map(ColorMode::toString).collect(Collectors.joining(","));

        logger.debug("[{}] DWC: supported color modes: [{}]", handler.getLogPrefix(), color_modes_str);

        configuration.put("color_modes", color_modes_str);

        Set<String> semanticTags = Set.of("Control", "Light");
        // DWC TODO - are RGB / Brightness / ON_OFF mutually exclusive?
        if (capabilities.contains(LightColorCapability.RGB)) {
            // Note: RGB gets converted to HSB for OpenHAB; a dimmer item linked to this channel will control the
            // brightness (B in HSB) of the color
            logger.debug("[{}] DWC: Detected RGB light", handler.getLogPrefix());

            logger.debug("[{}] DWC: Creating single COLOR channelType for RGB light with prefix '{}'",
                    handler.getLogPrefix(), LightColorCapabilityToStringMap.get(LightColorCapability.RGB));

            // Go for a single Color channel
            ChannelType channelType = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_COLOR;
            logger.debug("[{}] DWC: Creating COLOR channel for RBG light", handler.getLogPrefix());

            Channel channel = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "rgb"))
                    .withLabel(rsp.getName() + " RGB").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withAcceptedItemType(COLOR).withConfiguration(configuration).withDefaultTags(semanticTags).build();

            logger.debug("[{}] DWC: Registering COLOR channel", handler.getLogPrefix());

            super.registerChannel(channel, channelType);

            logger.debug("[{}] DWC: Channel registered", handler.getLogPrefix());
        }

        if (capabilities.contains(LightColorCapability.BRIGHTNESS)) {
            // Note: overall brightness of the light (separate from the color's brightness)
            logger.debug("[{}] DWC: Detected BRIGHTNESS light with response unique ID {} and response name {}",
                    handler.getLogPrefix(), rsp.getUniqueId(), rsp.getName());

            ChannelType channelType = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_BRIGHTNESS;
            Channel channel = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "brightness"))
                    .withLabel(rsp.getName() + " Brightness").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withAcceptedItemType(DIMMER).withConfiguration(configuration).build();

            super.registerChannel(channel, channelType);
        }

        if (capabilities.contains(LightColorCapability.ON_OFF)) {

            logger.debug("[{}] DWC: Detected ON_OFF light with response unique ID {} and response name {}",
                    handler.getLogPrefix(), rsp.getUniqueId(), rsp.getName());

            ChannelType channelType = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_ON_OFF;
            Channel channel = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "on_off"))
                    .withLabel(rsp.getName() + " Switch").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withAcceptedItemType(SWITCH).withConfiguration(configuration).build();

            super.registerChannel(channel, channelType);
        }

        if (capabilities.contains(LightColorCapability.COLOR_TEMPERATURE)) {

            float min_mireds = (float) rsp.getMinMireds();
            float max_mireds = (float) rsp.getMaxMireds();

            logger.debug(
                    "[{}] DWC: Detected COLOR_TEMPERATURE light with response unique ID {} and response name {}. Mired range: [{},{}]",
                    handler.getLogPrefix(), rsp.getUniqueId(), rsp.getName(), min_mireds, max_mireds);
            logger.debug("[{}] DWC: Saving min and max mireds in Configuration object", handler.getLogPrefix());

            configuration.put("min_mireds", min_mireds);
            configuration.put("max_mireds", max_mireds);

            ChannelType channelType = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_COLOR_TEMPERATURE;
            Channel channel = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "color_temperature"))
                    .withLabel(rsp.getName() + " Color Temperature").withKind(ChannelKind.STATE)
                    .withType(channelType.getUID()).withAcceptedItemType(DIMMER).withConfiguration(configuration)
                    .build();

            super.registerChannel(channel, channelType);
        }

        // if (color_modes.size() > 0) {
        {
            logger.debug("[{}] DWC: Adding color mode channel\n" + "Supported color modes: {}", handler.getLogPrefix(),
                    color_modes);

            ChannelType channelTypeCM = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_COLOR_MODE;
            logger.debug("[{}] DWC: Created ChannelType object channelTypeCM with UID {}", handler.getLogPrefix(),
                    channelTypeCM.getUID());
            Channel channelCM = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "color_mode"))
                    .withLabel(rsp.getName() + " Color Mode").withKind(ChannelKind.STATE)
                    .withType(channelTypeCM.getUID()).withAcceptedItemType(STRING).withConfiguration(configuration)
                    .build();

            logger.debug("[{}] DWC: Created Channel object channelCM with UID {}", handler.getLogPrefix(),
                    channelCM.getUID());

            logger.debug("[{}] DWC: Registering channel {}", handler.getLogPrefix(), channelCM.getUID());
            super.registerChannel(channelCM, channelTypeCM);

            logger.debug("[{}] DWC: Channel {} registered", handler.getLogPrefix(), channelCM.getUID());
        }
        // }

        if (capabilities.contains(LightColorCapability.COLD_WARM_WHITE)) {

            logger.debug("[{}] DWC: Detected COLD_WARM_WHITE light with response unique ID {} and response name {}",
                    handler.getLogPrefix(), rsp.getUniqueId(), rsp.getName());
            // Per documentation, this means one channel for the Cold White color temperature and one for the Warm White
            // color temp
            /* Warm White */
            ChannelType channelTypeWW = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_WARM_WHITE;
            logger.debug("[{}] DWC: Created ChannelType object channelTypeWW with UID {}", handler.getLogPrefix(),
                    channelTypeWW.getUID());
            Channel channelWW = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "warm_white"))
                    .withLabel(rsp.getName() + " Warm White").withKind(ChannelKind.STATE)
                    .withType(channelTypeWW.getUID()).withAcceptedItemType(DIMMER).withConfiguration(configuration)
                    .build();

            logger.debug("[{}] DWC: Created Channel object channelWW with UID {}", handler.getLogPrefix(),
                    channelWW.getUID());

            logger.debug("[{}] DWC: Registering channel {}", handler.getLogPrefix(), channelWW.getUID());
            super.registerChannel(channelWW, channelTypeWW);

            logger.debug("[{}] DWC: Channel {} registered", handler.getLogPrefix(), channelWW.getUID());
            /* Cold White */
            ChannelType channelTypeCW = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_COLD_WHITE;
            logger.debug("[{}] DWC: Created ChannelType object channelTypeCW with UID {}", handler.getLogPrefix(),
                    channelTypeCW.getUID());

            Channel channelCW = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "cold_white"))
                    .withLabel(rsp.getName() + " Cold White").withKind(ChannelKind.STATE)
                    .withType(channelTypeCW.getUID()).withAcceptedItemType(DIMMER).withConfiguration(configuration)
                    .build();

            logger.debug("[{}] DWC: Created Channel object channelCW with UID {}", handler.getLogPrefix(),
                    channelCW.getUID());

            logger.debug("[{}] DWC: Registering channel {}", handler.getLogPrefix(), channelCW.getUID());
            super.registerChannel(channelCW, channelTypeCW);

            logger.debug("[{}] DWC: Channel {} registered", handler.getLogPrefix(), channelCW.getUID());
        }

        if (rsp.getEffectsCount() > 0) {
            // Create effects channel
            // ChannelType channelType =
            // addChannelType(LightColorCapabilityToStringMap.get(LightColorCapability.EFFECTS),
            // rsp.getName(), STRING, semanticTags, icon, rsp.getEntityCategory(), rsp.getDisabledByDefault());
            ChannelType channelType = ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_EFFECTS;

            StateDescription stateDescription = optionListStateDescription(rsp.getEffectsList());

            // spotless:off
            Channel channel = ChannelBuilder.create(new ChannelUID(handler.getThing().getUID(), "effects"))
                    .withLabel(rsp.getName() + " Effects").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withAcceptedItemType(STRING)
                    .withConfiguration(configuration("Light", rsp.getKey(), LightColorCapabilityToStringMap.get(LightColorCapability.EFFECTS)))
                    .build();
            // spotless:on

            super.registerChannel(channel, channelType, stateDescription);

        }
    }

    public static String serialize(SortedSet<LightColorCapability> capabilities) {
        return capabilities.stream().map(e -> e.name()).collect(Collectors.joining(","));
    }

    public static SortedSet<LightColorCapability> deserialize(String capabilities) {
        return capabilities == null ? new TreeSet<>()
                : Arrays.stream(capabilities.split(",")).map(LightColorCapability::valueOf)
                        .collect(Collectors.toCollection(TreeSet::new));
    }

    private Optional<Channel> findChannelByChannelTypeUID(ChannelTypeUID channelTypeUID) {

        return handler.getThing().getChannels().stream()
                .filter(channel -> channel.getChannelTypeUID().equals(channelTypeUID)).findFirst();
    }

    /*
     * if (hasState):
     * for each channel in getChannels():
     * switch (channel.channelTypeUID):
     * case WARM_WHITE:
     * do warm white stuff
     * case COLD_WHITE:
     * do cold white stuff
     * case RBG:
     * do RGB stuff
     * etc, etc
     * 
     * 
     */
    // TODO: fix brightness/cold white/warm white/CT handling
    public void handleState(LightStateResponse rsp) {
        logger.debug("[{}] DWC: handleState() with:\n LightStateResponse key: {}\n Class '{}'", handler.getLogPrefix(),
                rsp.getKey(), rsp.getClass());

        logger.debug("[{}] DWC: handleState() with:\n rsp: '{}'", handler.getLogPrefix(),
                rsp.getAllFields().toString());
        ;

        logger.debug("[{}] DWC: handleState() with:\n getChannels: '{}'", handler.getLogPrefix(),
                handler.getThing().getChannels().toString());
        /*
         * if (!rsp.getState()) {
         * logger.debug("[{}] DWC: response has no state, setting to off", handler.getLogPrefix());
         * 
         * findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_ON_OFF)
         * .ifPresent(channel_on_off -> {
         * logger.debug("[{}] DWC: calling updateState with OnOffType.OFF", handler.getLogPrefix());
         * 
         * handler.updateState(channel_on_off.getUID(), State.);
         * });
         * 
         * 
         * } else
         */ {
            findChannelByKeyAndField(rsp.getKey(), CHANNEL_LIGHT).ifPresent(channel -> {
                Configuration configuration = channel.getConfiguration();
                SortedSet<LightColorCapability> capabilities = deserialize((String) configuration.get("capabilities"));
                logger.debug(
                        "[{}] DWC: findChannelByKeyAndField:\n" + " Channel: {}\n" + " ChannelTypeUID: {}\n"
                                + " Capabilities: {}\n" + " AcceptedItemType: {}\n" + " Properties: {}",
                        handler.getLogPrefix(), channel.getUID(), channel.getChannelTypeUID(),
                        (String) configuration.get("capabilities"), channel.getAcceptedItemType(),
                        channel.getProperties().toString());

                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE)
                        .ifPresent(channel_color_mode -> {
                            logger.debug(
                                    "[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_COLOR_MODE",
                                    handler.getLogPrefix());
                            // if (capabilities.contains(LightColorCapability.COLOR_MODE)) {
                            ColorMode color_mode = rsp.getColorMode();
                            logger.debug(
                                    "[{}] DWC: Processing Color Mode capability\n" + "getState(): {}\n"
                                            + " Color Mode: {}",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false", color_mode.toString());

                            handler.updateState(channel_color_mode.getUID(), new StringType(color_mode.toString()));
                            // }
                        });

                // if Brightness came in on the RGB Channel, we want to send it to the actual Brightness Channel,
                // since the RGB channel applies the color_brightness instead
                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS)
                        .ifPresent(channel_brightness -> {
                            logger.debug("[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_BRIGHTNESS",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false");
                            int brightness = (int) (rsp.getState() ? rsp.getBrightness() * 100 : 0);
                            PercentType percentType = new PercentType(brightness);
                            logger.debug(
                                    "[{}] DWC: Processing Brightness channel\n" + " getState(): {}\n"
                                            + " Brightness: {}\n" + " Has Brightness Capability: {}\n"
                                            + " Current Color Mode: {}",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false", percentType.floatValue(),
                                    capabilities.contains(LightColorCapability.BRIGHTNESS) ? "true" : "false",
                                    rsp.getColorMode().toString());
                            logger.debug("[{}] Posting state to channel_brightness", handler.getLogPrefix());
                            handler.updateState(channel_brightness.getUID(), percentType);

                        });

                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE)
                        .ifPresent((channel_cold_white -> {
                            logger.debug(
                                    "[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_COLD_WHITE",
                                    handler.getLogPrefix());

                            PercentType percentType = new PercentType(
                                    (int) (rsp.getState() ? rsp.getColdWhite() * 100 : 0));
                            logger.debug(
                                    "[{}] DWC: Processing Cold White capability\n" + "getState(): {}\n"
                                            + " COLD_WHITE: raw: {} / float: {}",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false", rsp.getColdWhite(),
                                    percentType.floatValue());
                            handler.updateState(channel_cold_white.getUID(), percentType);

                        }));

                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE)
                        .ifPresent((channel_warm_white -> {
                            logger.debug(
                                    "[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_WARM_WHITE",
                                    handler.getLogPrefix());

                            PercentType percentType = new PercentType(
                                    (int) (rsp.getState() ? rsp.getWarmWhite() * 100 : 0));
                            logger.debug(
                                    "[{}] DWC: Processing Warm White capability\n" + "getState(): {}\n"
                                            + " WARM_WHITE: raw: {} / float: {}",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false", rsp.getWarmWhite(),
                                    percentType.floatValue());
                            handler.updateState(channel_warm_white.getUID(), percentType);

                        }));

                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE)
                        .ifPresent((channel_color_temp -> {
                            logger.debug(
                                    "[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_COLOR_TEMPERATURE",
                                    handler.getLogPrefix());

                            BigDecimal min_mireds_obj = (BigDecimal) configuration.get("min_mireds");
                            BigDecimal max_mireds_obj = (BigDecimal) configuration.get("max_mireds");

                            logger.debug("[{} DWC: Class type of min_mireds_obj: {}]", handler.getLogPrefix(),
                                    min_mireds_obj.getClass());

                            // PercentType percentType = new PercentType(
                            // (int) (rsp.getState() ? rsp.getColorTemperature() * 100 : 0));
                            float x = rsp.getColorTemperature();
                            float y_min = 0;
                            float y_max = 100;
                            float x_max = max_mireds_obj.floatValue();
                            float x_min = min_mireds_obj.floatValue();

                            float y = y_min + (y_max - y_min) * ((x - x_min) / (x_max - x_min));

                            logger.debug(
                                    "[{}] DWC: Processing Color Temperature capability\n" + "getState(): {}\n"
                                            + " ColorTemp: raw: {} / float: {}\n" + " Scaled ColorTemp: {}\n"
                                            + " Config.min_mireds: {}\n" + " Config.max_mireds: {}",
                                    handler.getLogPrefix(), rsp.getState() ? "true" : "false",
                                    rsp.getColorTemperature(), x, y, x_min, x_max);

                            handler.updateState(channel_color_temp.getUID(), new PercentType((int) y));

                        }));

                findChannelByChannelTypeUID(ESPHomeChannelTypeProvider.ESPHOME_CHANNEL_TYPE_UID_ON_OFF)
                        .ifPresent(channel_on_off -> {
                            logger.debug(
                                    "[{}] DWC: Found channel with type UID ESPHOME_CHANNEL_TYPE_UID_ON_OFF",
                                    handler.getLogPrefix());
                            // if (capabilities.contains(LightColorCapability.COLOR_MODE)) {
                            OnOffType onOff = rsp.getState() ? OnOffType.ON : OnOffType.OFF;
                            logger.debug("[{}] DWC: Processing On/Off Channel with setting capability\n"
                                    + "getState(): {}\n" + " updateState to be called with: {}", handler.getLogPrefix(),
                                    onOff);

                            handler.updateState(channel_on_off.getUID(), onOff);
                            // }
                        });

                if (capabilities.contains(LightColorCapability.RGB)) {

                    logger.debug("[{}] DWC: findChannelByKeyAndField: Checking RGB (getState(): {})",
                            handler.getLogPrefix(), rsp.getState() ? "True" : "False");

                    if (rsp.getState()) {
                        PercentType percentTypeRGB[] = { new PercentType((int) (rsp.getRed() * 100)),
                                new PercentType((int) (rsp.getGreen() * 100)),
                                new PercentType((int) (rsp.getBlue() * 100)) };

                        PercentType percentTypeBrightness = new PercentType((int) (rsp.getBrightness() * 100));

                        PercentType percentTypeColorBrightness = new PercentType(
                                (int) (rsp.getColorBrightness() * 100));

                        logger.debug(
                                "[{}] DWC: findChannelByKeyAndField: processing RGB & ColorBrightness (?) capability\n"
                                        + " R: raw: {} / float: {}\n" + " G: raw: {} / float: {}\n"
                                        + " B: raw: {} / float: {}\n" + " Brightness: raw: {} / float: {}\n"
                                        + " Color Brightness: raw: {} / float: {}",
                                handler.getLogPrefix(), rsp.getRed(), percentTypeRGB[0].floatValue(), rsp.getGreen(),
                                percentTypeRGB[1].floatValue(), rsp.getBlue(), percentTypeRGB[2].floatValue(),
                                rsp.getBrightness(), percentTypeBrightness.floatValue(), rsp.getColorBrightness(),
                                percentTypeColorBrightness.floatValue());

                        PercentType percentTypeRGB2[] = {
                                new PercentType((int) ((rsp.getRed() * rsp.getBrightness()) * 100)),
                                new PercentType((int) ((rsp.getGreen() * rsp.getBrightness()) * 100)),
                                new PercentType((int) ((rsp.getBlue() * rsp.getBrightness()) * 100)) };
                        logger.debug(
                                "[{}] DWC: findChannelByKeyAndField: processing RGB & ColorBrightness (?) capability (2)\n"
                                        + " R2 float: {}\n" + " G2 float: {}\n" + " B2 float: {}\n",
                                handler.getLogPrefix(), percentTypeRGB2[0].floatValue(),
                                percentTypeRGB2[1].floatValue(), percentTypeRGB2[2].floatValue());

                        // Convert to color
                        HSBType hsbType = ColorUtil.rgbToHsb(percentTypeRGB);
                        HSBType hsbType2 = ColorUtil.rgbToHsb(percentTypeRGB2);

                        logger.debug(
                                "[{}] DWC: findChannelByKeyAndField: Converted HSB values:\n" + " H: {}\n" + " S: {}\n"
                                        + " B: {}",
                                handler.getLogPrefix(), hsbType.getHue().floatValue(),
                                hsbType.getSaturation().floatValue(), hsbType.getBrightness().floatValue());

                        logger.debug(
                                "[{}] DWC: findChannelByKeyAndField: Converted HSB values (2):\n" + " H: {}\n"
                                        + " S: {}\n" + " B: {}",
                                handler.getLogPrefix(), hsbType2.getHue().floatValue(),
                                hsbType2.getSaturation().floatValue(), hsbType2.getBrightness().floatValue());

                        // If off, set brightness to 0
                        // if (!rsp.getState()) {
                        // hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(0));
                        // } else {
                        // Adjust brightness
                        // logger.debug("[{}] DWC: findChannelByKeyAndField: Applying ColorBrightness {}",
                        // handler.getLogPrefix(), percentTypeColorBrightness.floatValue());
                        // hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), percentTypeColorBrightness);
                        // // }

                        if (rsp.getColorMode() == ColorMode.COLOR_MODE_RGB_COLD_WARM_WHITE) {
                            logger.debug("[{}] DWC: color mode rgb cold warm white", handler.getLogPrefix());
                            logger.debug("[{}] DWC: applying colorBrightness {} to HSB 1", handler.getLogPrefix(),
                                    percentTypeColorBrightness.floatValue());
                            hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(),
                                    percentTypeColorBrightness);
                        } else if (rsp.getColorMode() == ColorMode.COLOR_MODE_RGB) {
                            logger.debug("[{}] DWC: color mode rgb", handler.getLogPrefix());
                            logger.debug("[{}] DWC: applying brightness {} to HSB 1", handler.getLogPrefix(),
                                    channel.getClass(), percentTypeBrightness.floatValue());
                            hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), percentTypeBrightness);
                        }

                        if (((int) (rsp.getColorBrightness())) == 1) {
                            logger.debug("[{}] DWC: channel class {}\n" + " getColorBrightness == 1, so using HSB 2",
                                    handler.getLogPrefix(), channel.getClass());
                            hsbType = hsbType2;
                        } else {
                            // // logger.debug("[{}] DWC: getColorBrightness != 1, so applying colorBrightness {} to HSB
                            // // 1",
                            // // handler.getLogPrefix(), percentTypeColorBrightness.floatValue());
                            // // hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(),
                            // // percentTypeColorBrightness);
                            // // handler.getThing().

                            // logger.debug(
                            // "[{}] DWC: channel class {}\n\" + \" getColorBrightness != 1, so applying brightness {}
                            // to HSB 1",
                            // handler.getLogPrefix(), channel.getClass(),
                            // percentTypeColorBrightness.floatValue());
                            // hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(),
                            // percentTypeColorBrightness);
                        }

                        logger.debug(
                                "[{}] DWC: findChannelByKeyAndField: Final caclulated HSB values:\n" + " H: {}\n"
                                        + " S: {}\n" + " B: {}",
                                handler.getLogPrefix(), hsbType.getHue().floatValue(),
                                hsbType.getSaturation().floatValue(), hsbType.getBrightness().floatValue());

                        handler.updateState(channel.getUID(), hsbType);
                    } else {
                        logger.debug("[{}] DWC: state is false, no-op", handler.getLogPrefix());
                    }
                }

                // TODO: confirm if ON_OFF should only be used for lights that do not have a brightness or color
                // brightness
                // capability
                // Tested: if light is turned off, brightness goes to 0 on its own
                if (capabilities.contains(LightColorCapability.ON_OFF)
                        && !(capabilities.contains(LightColorCapability.RGB))) {
                    OnOffType onOff = rsp.getState() ? OnOffType.ON : OnOffType.OFF;
                    logger.debug(
                            "[{}] DWC: findChannelByKeyAndField: processing On/Off capability\n" + "getState(): {}\n"
                                    + " ON_OFF: {}",
                            handler.getLogPrefix(), rsp.getState() ? "true" : "false",
                            onOff.equals(OnOffType.ON) ? "ON" : "OFF");
                    handler.updateState(channel.getUID(), onOff);
                }
            });

            findChannelByKeyAndField(rsp.getKey(), CHANNEL_EFFECTS).ifPresent(channel -> {
                handler.updateState(channel.getUID(), new StringType(rsp.getEffect()));
            });
        }
    }

    private SortedSet<LightColorCapability> decodeCapabilities(ListEntitiesLightResponse rsp) {
        SortedSet<LightColorCapability> capabilities = new TreeSet<>();
        for (Integer bitset : rsp.getSupportedColorModesValueList()) {
            if ((bitset & (1 << 0)) != 0) {
                capabilities.add(LightColorCapability.ON_OFF);
            }
            if ((bitset & (1 << 1)) != 0) {
                capabilities.add(LightColorCapability.BRIGHTNESS);
            }
            if ((bitset & (1 << 2)) != 0) {
                capabilities.add(LightColorCapability.WHITE);
            }
            if ((bitset & (1 << 3)) != 0) {
                capabilities.add(LightColorCapability.COLOR_TEMPERATURE);
            }
            if ((bitset & (1 << 4)) != 0) {
                capabilities.add(LightColorCapability.COLD_WARM_WHITE);
            }
            if ((bitset & (1 << 5)) != 0) {
                capabilities.add(LightColorCapability.RGB);
            }
        }
        return capabilities;
    }

    public enum LightColorCapability {
        ON_OFF,
        BRIGHTNESS,
        WHITE,
        COLOR_TEMPERATURE,
        COLD_WARM_WHITE,
        COLD_WHITE,
        WARM_WHITE,
        RGB,
        EFFECTS,
        COLOR_MODE
    }

    // Turn off spotless formatting for these two Maps to keep the key/value pairs in the code readable
    // spotless:off
    private static final Map<LightColorCapability, String> LightColorCapabilityToStringMap = Map.of(
            LightColorCapability.ON_OFF, "switch",
            LightColorCapability.BRIGHTNESS, "brightness",
            LightColorCapability.WHITE, "white",
            LightColorCapability.COLOR_TEMPERATURE, "color_temperature",
            LightColorCapability.COLD_WARM_WHITE,"cold_warm_white", 
            LightColorCapability.WARM_WHITE, "warm_white",
            LightColorCapability.COLD_WHITE, "cold_white",
            LightColorCapability.RGB, "rgb",
            LightColorCapability.EFFECTS, "effects",
            LightColorCapability.COLOR_MODE, "color_mode"
    );

    private static final Map<String, LightColorCapability> LightColorStringToCapabilityMap = Map.of(
            "switch", LightColorCapability.ON_OFF,
            "brightness", LightColorCapability.BRIGHTNESS,
            "white", LightColorCapability.WHITE,
            "color_temperature", LightColorCapability.COLOR_TEMPERATURE,
            "cold_warm_white", LightColorCapability.COLD_WARM_WHITE,
            "warm_white", LightColorCapability.WARM_WHITE,
            "cold_white", LightColorCapability.COLD_WHITE,
            "rgb", LightColorCapability.RGB,
            "effects", LightColorCapability.EFFECTS,
            "color_mode", LightColorCapability.COLOR_MODE
    );
    // spotless:on
}
