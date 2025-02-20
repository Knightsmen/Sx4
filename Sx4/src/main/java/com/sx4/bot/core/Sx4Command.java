package com.sx4.bot.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jockie.bot.core.command.impl.CommandImpl;
import com.sx4.bot.interfaces.Donator;
import com.sx4.bot.interfaces.Example;
import com.sx4.bot.translations.CommandTranslation;
import com.sx4.bot.translations.TranslationType;

public class Sx4Command extends CommandImpl {
	
	protected boolean donator = false;
	
	protected String example = null;
	
	protected List<CommandTranslation> commandTranslations;
	
	protected boolean disabled = false;
	protected String disabledMessage = null;
	
	public Sx4Command(String name) {
		super(name, true);
		
		this.doAnnotations();
		
		if (this.commandTranslations == null) {
			this.commandTranslations = new ArrayList<>();
		}
	}
	
	public Sx4Command(String name, Method method, Object invoker) {
		super(name, method, invoker);
		
		this.doAnnotations();
		
		if (this.commandTranslations == null) {
			this.commandTranslations = new ArrayList<>();
		}
	}
	
	public CommandTranslation getCommandTranslation() {
		return this.getCommandTranslation(TranslationType.EN_GB);
	}
	
	public CommandTranslation getCommandTranslation(TranslationType translationType) {
		if (this.commandTranslations == null) {
			this.commandTranslations = new ArrayList<>();
		}
		
		for (CommandTranslation commandTranslation : this.commandTranslations) {
			if (commandTranslation.getTranslationType().equals(translationType)) {
				return commandTranslation;
			}
		}
		
		return null;
	}
	
	public String getDescription() {
		return this.getDescription(TranslationType.EN_GB);
	}
	
	public String getDescription(TranslationType translationType) {
		CommandTranslation commandTranslation = this.getCommandTranslation(translationType);
		if (commandTranslation == null) {
			commandTranslation = this.getCommandTranslation();
		}
		
		return commandTranslation == null ? null : commandTranslation.getDescription();
	}
	
	public Sx4Command setDescription(String description) {
		return this.setDescription(TranslationType.EN_GB, description);
	}
	
	public Sx4Command setDescription(TranslationType translationType, String description) {
		if (this.commandTranslations == null) {
			this.commandTranslations = new ArrayList<>();
		}
		
		for (CommandTranslation commandTranslation : this.commandTranslations) {
			if (commandTranslation.getTranslationType().equals(translationType)) {
				commandTranslation.setDescription(description);
				
				return this;
			}
		}
		
		this.commandTranslations.add(new CommandTranslation(translationType, description, null));
		
		return this;
	}
	
	public Map<String, String> getStrings() {
		return this.getStrings(TranslationType.EN_GB);
	}
	
	public Map<String, String> getStrings(TranslationType translationType) {
		CommandTranslation commandTranslation = this.getCommandTranslation(translationType);
		if (commandTranslation == null) {
			commandTranslation = this.getCommandTranslation();
		}
		
		return commandTranslation == null ? null : commandTranslation.getStrings();
	}
	
	public Sx4Command setStrings(Map<String, String> strings) {
		return this.setStrings(TranslationType.EN_GB, strings);
	}
	
	public Sx4Command setStrings(TranslationType translationType, Map<String, String> strings) {
		for (CommandTranslation commandTranslation : this.commandTranslations) {
			if (commandTranslation.getTranslationType().equals(translationType)) {
				commandTranslation.setStrings(strings);
				
				return this;
			}
		}
		
		this.commandTranslations.add(new CommandTranslation(translationType, null, strings));
		
		return this;
	}
	
	public void enable() {
		this.disabled = false;
		this.disabledMessage = null;
	}
	
	public void disable() {
		this.disable(null);
	}
	
	public void disable(String message) {
		this.disabled = true;
		this.disabledMessage = message;
	}
	
	public boolean isDisabled() {
		return this.disabled;
	}
	
	public boolean hasDisabledMessage() {
		return this.disabledMessage != null;
	}
	
	public String getDisabledMessage() {
		return this.disabledMessage;
	}
	
	public String getExample() {
		return this.example;
	}
	
	public Sx4Command setExample(String example) {
		this.example = example;
		
		return this;
	}
	
	public boolean isDonator() {
		return this.donator;
	}
	
	public Sx4Command setDonator(boolean donator) {
		this.donator = donator;
		
		return this;
	}
	
	private void doAnnotations() {
		if (this.method != null) {
			if (this.method.isAnnotationPresent(Donator.class)) {
				this.donator = this.method.getAnnotation(Donator.class).value();
			} 
			
			if (this.method.isAnnotationPresent(Example.class)) {
				this.example = this.method.getAnnotation(Example.class).value();
			}
		}
	}
}
