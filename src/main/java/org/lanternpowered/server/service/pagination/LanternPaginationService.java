package org.lanternpowered.server.service.pagination;

import static org.lanternpowered.server.text.translation.TranslationHelper.t;
import static org.spongepowered.api.util.command.args.GenericArguments.integer;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;

import org.lanternpowered.server.entity.living.player.LanternPlayer;
import org.lanternpowered.server.game.LanternGame;
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationCalculator;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.args.CommandArgs;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.args.CommandElement;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public class LanternPaginationService implements PaginationService {

    static class SourcePaginations {
        private final Map<UUID, ActivePagination> paginations = new ConcurrentHashMap<UUID, ActivePagination>();
        private volatile UUID lastUuid;

        public ActivePagination get(UUID uuid) {
            return this.paginations.get(uuid);
        }

        public void put(ActivePagination pagination) {
            synchronized (this.paginations) {
                this.paginations.put(pagination.getId(), pagination);
                this.lastUuid = pagination.getId();
            }
        }

        public Set<UUID> keys() {
            return this.paginations.keySet();
        }

        public UUID getLastUuid() {
            return this.lastUuid;
        }
    }
    final ConcurrentMap<Class<? extends CommandSource>, PaginationCalculator<?>> calculators = new ConcurrentHashMap<Class<? extends
            CommandSource>, PaginationCalculator<?>>();
    final ConcurrentMap<CommandSource, SourcePaginations> activePaginations = new MapMaker().weakKeys().makeMap();
    private final AtomicBoolean commandRegistered = new AtomicBoolean();
    private final LanternGame game;

    public LanternPaginationService(LanternGame game) {
        this.game = game;
        this.setPaginationCalculator(LanternPlayer.class, new PlayerPaginationCalculator());
    }

    void registerCommandOnce() {
        if (this.commandRegistered.compareAndSet(false, true)) {
            this.game.getCommandDispatcher().register(LanternGame.plugin(), CommandSpec.builder()
                    .description(t("Helper command for paginations occurring"))
                    .arguments(new ActivePaginationCommandElement(t("pagination-id")))
                    .child(CommandSpec.builder()
                            .description(t("Go to the next page"))
                            .executor(new CommandExecutor() {
                                @Override
                                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                                    args.<ActivePagination>getOne("pagination-id").get().nextPage();
                                    return CommandResult.success();
                                }
                            }).build(), "next", "n")
                    .child(CommandSpec.builder()
                            .description(t("Go to the previous page"))
                            .executor(new CommandExecutor() {
                                @Override
                                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                                    args.<ActivePagination>getOne("pagination-id").get().previousPage();
                                    return CommandResult.success();
                                }
                            }).build(), "previous", "prev", "p")
                    .child(CommandSpec.builder()
                            .description(t("Go to a specific page"))
                            .arguments(integer(t("page")))
                            .executor(new CommandExecutor() {
                                @Override
                                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                                    args.<ActivePagination>getOne("pagination-id").get().specificPage(args.<Integer>getOne("page").get());
                                    return CommandResult.success();
                                }
                            }).build(), "page")
                    .build(), "pagination", "page");
        }

    }

    @Override
    public PaginationBuilder builder() {
        return new LanternPaginationBuilder(this);
    }

    @Override
    public <T extends CommandSource> void setPaginationCalculator(Class<T> type, PaginationCalculator<? super T> calculator) throws
            IllegalArgumentException {
        PaginationCalculator<?> existing = this.calculators.putIfAbsent(type, calculator);
        if (existing != null) {
            throw new IllegalArgumentException("Pagination calculator already registered for the type " + type);
        }
    }

    private static final PaginationCalculator<CommandSource> UNPAGINATED_CALCULATOR = new FixedLengthPaginationCalculator(-1);

    @Override
    public PaginationCalculator<CommandSource> getUnpaginatedCalculator() {
        return UNPAGINATED_CALCULATOR;
    }

    @Override
    public PaginationCalculator<CommandSource> getFixedLinesCalculator(int lines) {
        return new FixedLengthPaginationCalculator(lines);
    }

    SourcePaginations getPaginationState(CommandSource source, boolean create) {
        SourcePaginations ret = this.activePaginations.get(source);
        if (ret == null && create) {
            ret = new SourcePaginations();
            SourcePaginations existing = this.activePaginations.putIfAbsent(source, ret);
            if (existing != null) {
                ret = existing;
            }
        }
        return ret;
    }

    private class ActivePaginationCommandElement extends CommandElement {

        protected ActivePaginationCommandElement(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            UUID id;
            SourcePaginations paginations = getPaginationState(source, false);
            if (paginations == null) {
                throw args.createError(t("Source %s has no paginations!", source));
            }

            Object state = args.getState();
            try {
                id = UUID.fromString(args.next());
            } catch (IllegalArgumentException ex) { // TODO: Just use last valid input?
                if (paginations.getLastUuid() != null) {
                    args.setState(state);
                    return paginations.get(paginations.getLastUuid());
                } else {
                    throw args.createError(t("Input was not a valid UUID!"));
                }
            }
            ActivePagination pagination = paginations.get(id);
            if (pagination == null) {
                throw args.createError(t("No pagination registered for id %s", id.toString()));
            }
            return paginations.get(id);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            SourcePaginations paginations = getPaginationState(src, false);
            if (paginations == null) {
                return ImmutableList.of();
            }

            final Optional<String> optNext = args.nextIfPresent();
            if (optNext.isPresent()) {
                return ImmutableList.copyOf(Iterables.filter(
                        Iterables.transform(paginations.keys(), Functions.toStringFunction()), new StartsWithPredicate(optNext.get())));
            } else {
                return ImmutableList.copyOf(Iterables.transform(paginations.keys(), Functions.toStringFunction()));
            }
        }
    }
}