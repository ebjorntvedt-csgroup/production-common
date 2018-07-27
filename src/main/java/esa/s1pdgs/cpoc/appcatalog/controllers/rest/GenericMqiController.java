package esa.s1pdgs.cpoc.appcatalog.controllers.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import esa.s1pdgs.cpoc.appcatalog.model.MqiMessage;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericReadMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiLightMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiSendMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.appcatalog.services.mongodb.MongoDBServices;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;

/**
 * @author Viveris Technologies
 * @param <T>
 */
public class GenericMqiController<T> {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(GenericMqiController.class);

    /**
     * Service for managing MQI messages TODO rename class and attribute
     */
    private final MongoDBServices mongoDBServices;

    /**
     * TODO: not use here: a controller check only the input and buils the
     * ouput, the intelligence is in a service
     */
    private final int maxRetries;

    /**
     * PRoduct category
     */
    private final ProductCategory category;

    /**
     * Constructor
     * 
     * @param mongoDBServices
     * @param maxRetries
     * @param category
     */
    public GenericMqiController(final MongoDBServices mongoDBServices,
            final int maxRetries, final ProductCategory category) {
        this.mongoDBServices = mongoDBServices;
        this.maxRetries = maxRetries;
        this.category = category;
    }

    /**
     * Internal function to log messages
     * 
     * @param message
     */
    private void log(final String message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message);
        }
    }

    /**
     * @param topic
     * @param partition
     * @param offset
     * @param body
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{topic}/{partition}/{offset}/read")
    public ResponseEntity<MqiLightMessageDto> readMessage(
            @PathVariable(name = "topic") final String topic,
            @PathVariable(name = "partition") final int partition,
            @PathVariable(name = "offset") final long offset,
            @RequestBody final MqiGenericReadMessageDto<T> body) {

        log(String.format(
                "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] Searching MqiMessage",
                topic, partition, offset, body.getGroup()));
        List<MqiMessage> responseFromDB =
                mongoDBServices.searchByTopicPartitionOffsetGroup(topic,
                        partition, offset, body.getGroup());

        // Si un objet n'existe pas dans la BDD avec topic / partition / offset
        // / group
        if (responseFromDB.isEmpty()) {
            // On créer le message dans la BDD
            log(String.format(
                    "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] Inserting new MqiMessage",
                    topic, partition, offset, body.getGroup()));
            MqiMessage messageToInsert = new MqiMessage(category, topic,
                    partition, offset, body.getGroup(),
                    MqiStateMessageEnum.READ, body.getPod(), new Date(), null,
                    null, null, 0, body.getDto());
            mongoDBServices.insertMqiMessage(messageToInsert);

            // On renvoie le message que l'on vient de créer
            return new ResponseEntity<MqiLightMessageDto>(
                    transformMqiMessageToMqiLightMessage(messageToInsert),
                    HttpStatus.OK);
        } else { // Sinon on récupère le premier de la liste
            log(String.format(
                    "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] Found MqiMessage",
                    topic, partition, offset, body.getGroup()));
            MqiMessage messageFromDB = responseFromDB.get(0);
            // Si l'état est à ACK
            if (messageFromDB.getState().equals(MqiStateMessageEnum.ACK_OK)
                    || messageFromDB.getState()
                            .equals(MqiStateMessageEnum.ACK_KO)
                    || messageFromDB.getState()
                            .equals(MqiStateMessageEnum.ACK_WARN)) {
                // on renvoie l’objet
                log(String.format(
                        "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] MqiMessage is Acknowledge",
                        topic, partition, offset, body.getGroup()));
                return new ResponseEntity<MqiLightMessageDto>(
                        transformMqiMessageToMqiLightMessage(messageFromDB),
                        HttpStatus.OK);
            } else if (body.isForce()) { // sinon si force = true
                log(String.format(
                        "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] Force is true",
                        topic, partition, offset, body.getGroup()));
                HashMap<String, Object> updateMap = new HashMap<>();
                //  on incrémente nb_retry
                messageFromDB.setNbRetries(messageFromDB.getNbRetries() + 1);
                updateMap.put("nbRetries", messageFromDB.getNbRetries());
                if (messageFromDB.getNbRetries() == maxRetries) {
                    // on publie un message d’erreur dans queue (via mqi du
                    // catalogue)
                    // TODO
                    LOGGER.error(
                            "[Read Message] [Topic {}] [Partition {}] [Offset {}] [Body {}] Number of retries is reached",
                            topic, partition, offset, body.getGroup());
                    // on met status = ACK_KO
                    messageFromDB.setState(MqiStateMessageEnum.ACK_KO);
                    updateMap.put("state", messageFromDB.getState());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastAckDate(now);
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastAckDate", now);
                    updateMap.put("lastReadDate", now);
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier(),
                            updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(
                            transformMqiMessageToMqiLightMessage(messageFromDB),
                            HttpStatus.OK);
                } else {
                    log(String.format(
                            "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] Number of retries is not reached",
                            topic, partition, offset, body.getGroup()));
                    // on met status = READ
                    messageFromDB.setState(MqiStateMessageEnum.READ);
                    updateMap.put("state", messageFromDB.getState());
                    // on met le reading_pod au pod recu
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());
                    // on met le processing_pod à null
                    messageFromDB.setSendingPod(null);
                    updateMap.put("sendingPod", messageFromDB.getSendingPod());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastSendDate(now);
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastSendDate", now);
                    updateMap.put("lastReadDate", now);
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier(),
                            updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(
                            transformMqiMessageToMqiLightMessage(messageFromDB),
                            HttpStatus.OK);
                }
            } else {
                HashMap<String, Object> updateMap = new HashMap<>();
                if (messageFromDB.getState().equals(MqiStateMessageEnum.READ)) {
                    log(String.format(
                            "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] MqiMessage is at State READ",
                            topic, partition, offset, body.getGroup()));
                    // on met à jour les éventuelles dates et le reading_pod
                    Date now = new Date();
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastReadDate", now);
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier(),
                            updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(
                            transformMqiMessageToMqiLightMessage(messageFromDB),
                            HttpStatus.OK);
                }
                if (messageFromDB.getState().equals(MqiStateMessageEnum.SEND)) {
                    log(String.format(
                            "[Read Message] [Topic %s] [Partition %d] [Offset %d] [Body %s] MqiMessage is at State SEND",
                            topic, partition, offset, body.getGroup()));
                    // on met à jour les éventuelles dates et le reading_pod
                    Date now = new Date();
                    messageFromDB.setLastSendDate(now);
                    updateMap.put("lastSendDate", now);
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier(),
                            updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(
                            transformMqiMessageToMqiLightMessage(messageFromDB),
                            HttpStatus.OK);
                }
            }
        }
        LOGGER.error(
                "[Read Message] [Topic {}] [Partition {}] [Offset {}] [Body {}] ERROR",
                topic, partition, offset, body.getGroup());
        return new ResponseEntity<MqiLightMessageDto>(HttpStatus.NOT_FOUND);

    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/next")
    public ResponseEntity<List<MqiGenericMessageDto<T>>> next(
            @RequestParam("pod") final String pod) {
        Set<MqiStateMessageEnum> ackStates = new HashSet<>();
        ackStates.add(MqiStateMessageEnum.ACK_KO);
        ackStates.add(MqiStateMessageEnum.ACK_OK);
        ackStates.add(MqiStateMessageEnum.ACK_WARN);
        log(String.format(
                "[Next] [Pod %s] [States %s] [Product Category %s] Searching MqiMessage",
                pod, ackStates, category));
        List<MqiMessage> mqiMessages = mongoDBServices
                .searchByPodStateCategory(pod, category, ackStates);
        if (mqiMessages.isEmpty()) {
            log(String.format(
                    "[Next] [Pod %s] [States %s] [Product Category %s] No MqiMessage found",
                    pod, ackStates, category));
            return new ResponseEntity<List<MqiGenericMessageDto<T>>>(
                    new ArrayList<MqiGenericMessageDto<T>>(), HttpStatus.OK);
        } else {
            log(String.format(
                    "[Next] [Pod %s] [States %s] [Product Category %s] Returning list of found MqiMessage",
                    pod, ackStates, category));
            List<MqiGenericMessageDto<T>> messagesToReturn = new ArrayList<>();
            mqiMessages.forEach(x -> messagesToReturn
                    .add(transformMqiMessageToDtoGenericMessage(x)));
            return new ResponseEntity<List<MqiGenericMessageDto<T>>>(
                    messagesToReturn, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{messageID}/send")
    public ResponseEntity<Boolean> sendMessage(
            @PathVariable(name = "messageID") final long messageID,
            @RequestBody final MqiSendMessageDto body) {

        log(String.format("[Send Message] [MessageID %d] Searching MqiMessage",
                messageID));
        List<MqiMessage> responseFromDB = mongoDBServices.searchByID(messageID);

        if (responseFromDB.isEmpty()) {
            LOGGER.error("[Send Message] [MessageID {}] No MqiMessage found",
                    messageID);
            return new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND);
        } else { // Si le message existe
            MqiMessage messageFromDB = responseFromDB.get(0);
            Date now = new Date();
            switch (messageFromDB.getState()) {
                case ACK_KO:
                case ACK_OK:
                case ACK_WARN:
                    log(String.format(
                            "[Send Message] [MessageID %d] MqiMessage found is at state ACK",
                            messageID));
                    return new ResponseEntity<Boolean>(Boolean.FALSE,
                            HttpStatus.OK);
                case READ:
                    HashMap<String, Object> updateMap1 = new HashMap<>();
                    // on met status à SEND et son processing_pod
                    messageFromDB.setState(MqiStateMessageEnum.SEND);
                    messageFromDB.setSendingPod(body.getPod());
                    updateMap1.put("state", messageFromDB.getState());
                    updateMap1.put("sendingPod", messageFromDB.getSendingPod());
                    // on met à jour les éventuelles dates
                    messageFromDB.setLastAckDate(now);
                    messageFromDB.setLastSendDate(now);
                    updateMap1.put("lastAckDate", now);
                    mongoDBServices.updateByID(messageID, updateMap1);
                    log(String.format(
                            "[Send Message] [MessageID %d] MqiMessage found is at state READ",
                            messageID));
                    return new ResponseEntity<Boolean>(Boolean.TRUE,
                            HttpStatus.OK);
                default:
                    HashMap<String, Object> updateMap2 = new HashMap<>();
                    //  on incrémente nb_retry
                    messageFromDB
                            .setNbRetries(messageFromDB.getNbRetries() + 1);
                    updateMap2.put("nbRetries", messageFromDB.getNbRetries());
                    if (messageFromDB.getNbRetries() >= maxRetries) {
                        // on publie un message d’erreur dans queue (via mqi du
                        // catalogue)
                        // TODO
                        LOGGER.error(
                                "[Send Message] [MessageID {}] Number of retries is not reached",
                                messageID);
                        // on met status = ACK_KO
                        messageFromDB.setState(MqiStateMessageEnum.ACK_KO);
                        updateMap2.put("state", messageFromDB.getState());
                        // on met à jour les éventuelles dates
                        messageFromDB.setLastAckDate(now);
                        updateMap2.put("lastAckDate", now);
                        mongoDBServices.updateByID(messageID, updateMap2);
                        return new ResponseEntity<Boolean>(Boolean.FALSE,
                                HttpStatus.OK);
                    } else {
                        // on met status = à SEND et son processing_pod
                        messageFromDB.setState(MqiStateMessageEnum.SEND);
                        messageFromDB.setSendingPod(body.getPod());
                        updateMap2.put("state", messageFromDB.getState());
                        updateMap2.put("sendingPod",
                                messageFromDB.getSendingPod());
                        // on met à jour les éventuelles dates
                        messageFromDB.setLastSendDate(now);
                        updateMap2.put("lastSendDate", now);
                        mongoDBServices.updateByID(messageID, updateMap2);
                        log(String.format(
                                "[Send Message] [MessageID %d] MqiMessage found state is set at SEND",
                                messageID));
                        return new ResponseEntity<Boolean>(Boolean.TRUE,
                                HttpStatus.OK);
                    }

            }
        }
    }
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{messageID}/ack")
    public ResponseEntity<MqiGenericMessageDto<T>> ackMessage(
            @PathVariable(name = "messageID") final long messageID,
            @RequestBody final Ack ack) {

        HashMap<String, Object> updateMap = new HashMap<>();
        if (ack.equals(Ack.OK)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_OK);
        } else if (ack.equals(Ack.ERROR)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_KO);
        } else if (ack.equals(Ack.WARN)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_WARN);
        } else {
            LOGGER.error(
                    "[Ack Message] [MessageID {}] [Ack {}] Ack is not valid",
                    messageID, ack);
            return new ResponseEntity<MqiGenericMessageDto<T>>(
                    HttpStatus.NOT_FOUND);
        }
        Date now = new Date();
        updateMap.put("lastAckDate", now);

        mongoDBServices.updateByID(messageID, updateMap);
        List<MqiMessage> responseFromDB = mongoDBServices.searchByID(messageID);
        // on met le status à ak_ok ou ack_ko

        if (responseFromDB.isEmpty()) {
            LOGGER.error(
                    "[Ack Message] [MessageID {}] [Ack {}] No MqiMessage Found with MessageID",
                    messageID, ack);
            return new ResponseEntity<MqiGenericMessageDto<T>>(
                    HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<MqiGenericMessageDto<T>>(
                    transformMqiMessageToDtoGenericMessage(
                            responseFromDB.get(0)),
                    HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{topic}/{partition}/earliestOffset")
    public ResponseEntity<Long> earliestOffset(
            @PathVariable(name = "topic") final String topic,
            @PathVariable(name = "partition") final int partition,
            @RequestParam("group") final String group) {

        // Pour le topic / partition / group donné, on récupère l’offset du
        // message avec status != ACK et la plus petite date de lecture (à voir
        // si on prend le plus petit offset)
        Set<MqiStateMessageEnum> ackStates = new HashSet<>();
        ackStates.add(MqiStateMessageEnum.ACK_KO);
        ackStates.add(MqiStateMessageEnum.ACK_OK);
        ackStates.add(MqiStateMessageEnum.ACK_WARN);
        List<MqiMessage> responseFromDB =
                mongoDBServices.searchByTopicPartitionGroup(topic, partition,
                        group, ackStates);
        if (responseFromDB.isEmpty()) {
            // TODO define the strategy
            // Si pas d’entrée, on renvoie valeur par défaut :
            // -2 : on laisse le consumer faire ce qu’il veut
            // -1 : on démarre à l’offset du début
            // 0 : on démarre à l’offset de fin
            log(String.format(
                    "[EarliestOffset] [Topic %s] [Partition %d] [Group %s] Returning default Strategy",
                    topic, partition, group));
            return new ResponseEntity<Long>(Long.valueOf(0), HttpStatus.OK);
        } else {
            log(String.format(
                    "[EarliestOffset] [Topic %s] [Partition %d] [Group %s] Returning earlist offset",
                    topic, partition, group));
            return new ResponseEntity<Long>(responseFromDB.get(0).getOffset(),
                    HttpStatus.OK);
        }
    }

    private MqiLightMessageDto transformMqiMessageToMqiLightMessage(
            final MqiMessage messageToTransform) {
        MqiLightMessageDto messageTransformed = new MqiLightMessageDto();
        messageTransformed.setCategory(messageToTransform.getCategory());
        messageTransformed.setGroup(messageToTransform.getGroup());
        messageTransformed.setIdentifier(messageToTransform.getIdentifier());
        messageTransformed.setLastAckDate(messageToTransform.getLastAckDate());
        messageTransformed
                .setLastReadDate(messageToTransform.getLastReadDate());
        messageTransformed
                .setLastSendDate(messageToTransform.getLastSendDate());
        messageTransformed.setNbRetries(messageToTransform.getNbRetries());
        messageTransformed.setOffset(messageToTransform.getOffset());
        messageTransformed.setPartition(messageToTransform.getPartition());
        messageTransformed.setReadingPod(messageToTransform.getReadingPod());
        messageTransformed.setSendingPod(messageToTransform.getSendingPod());
        messageTransformed.setState(messageToTransform.getState());
        messageTransformed.setTopic(messageToTransform.getTopic());
        return messageTransformed;
    }

    @SuppressWarnings("unchecked")
    private MqiGenericMessageDto<T> transformMqiMessageToDtoGenericMessage(
            final MqiMessage messageToTransform) {
        MqiGenericMessageDto<T> messageTransformed =
                new MqiGenericMessageDto<T>();
        messageTransformed.setCategory(messageToTransform.getCategory());
        messageTransformed.setGroup(messageToTransform.getGroup());
        messageTransformed.setIdentifier(messageToTransform.getIdentifier());
        messageTransformed.setLastAckDate(messageToTransform.getLastAckDate());
        messageTransformed
                .setLastReadDate(messageToTransform.getLastReadDate());
        messageTransformed
                .setLastSendDate(messageToTransform.getLastSendDate());
        messageTransformed.setNbRetries(messageToTransform.getNbRetries());
        messageTransformed.setOffset(messageToTransform.getOffset());
        messageTransformed.setPartition(messageToTransform.getPartition());
        messageTransformed.setReadingPod(messageToTransform.getReadingPod());
        messageTransformed.setSendingPod(messageToTransform.getSendingPod());
        messageTransformed.setState(messageToTransform.getState());
        messageTransformed.setTopic(messageToTransform.getTopic());
        messageTransformed.setDto((T) messageToTransform.getDto());
        return messageTransformed;
    }

}
