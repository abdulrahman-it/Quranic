import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface AthkarCardProps {
  type: 'morning' | 'evening';
  title: string;
  subtitle: string;
  timeRange: string;
  onPress: () => void;
}

export const AthkarCard: React.FC<AthkarCardProps> = ({
  type,
  title,
  subtitle,
  timeRange,
  onPress,
}) => {
  const isMorning = type === 'morning';
  const accentColor = isMorning ? SemanticLight.secondary : '#4A6984';
  const bgColor = isMorning ? '#FAF7F0' : '#F1F5F8';

  return (
    <TouchableOpacity
      style={[styles.container, { backgroundColor: bgColor }]}
      activeOpacity={0.85}
      onPress={onPress}
    >
      <View style={[styles.glowBar, { backgroundColor: accentColor }]} />
      <View style={styles.body}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.subtitle}>{subtitle}</Text>
        <View style={styles.footer}>
          <Text style={[styles.timeRange, { color: accentColor }]}>{timeRange}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: SemanticLight.border,
    position: 'relative',
    overflow: 'hidden',
  },
  glowBar: {
    height: 4,
    left: 0,
    right: 0,
    top: 0,
    position: 'absolute',
  },
  body: {
    padding: 16,
    alignItems: 'flex-end',
  },
  title: {
    fontSize: 16,
    color: SemanticLight.textPrimary,
    fontWeight: 'bold',
    writingDirection: 'rtl',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 12,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
    marginBottom: 12,
  },
  footer: {
    alignSelf: 'stretch',
    alignItems: 'flex-start',
  },
  timeRange: {
    fontSize: 11,
    fontWeight: '900',
    writingDirection: 'rtl',
  },
});
